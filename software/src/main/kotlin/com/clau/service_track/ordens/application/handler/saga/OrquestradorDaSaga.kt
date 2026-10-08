package com.clau.service_track.ordens.application.handler.saga

import com.clau.service_track.ordens.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.CancelarOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.CancelarOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.FinalizarOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.FinalizarOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarExecucaoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarExecucaoUseCase
import com.clau.service_track.ordens.application.port.out.mensageria.FabricaDeComandoDeEstoquePort
import com.clau.service_track.ordens.application.port.out.mensageria.OutboxPort
import com.clau.service_track.ordens.application.port.out.repository.OrdemServicoRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.SagaRepositoryPort
import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.saga.Saga
import com.clau.service_track.ordens.domain.saga.SituacaoDaSaga
import com.clau.service_track.ordens.domain.saga.TipoDeSaga
import java.time.Duration
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

open class OrquestradorDaSaga(
    private val sagas: SagaRepositoryPort,
    private val ordens: OrdemServicoRepositoryPort,
    private val outbox: OutboxPort,
    private val comandos: FabricaDeComandoDeEstoquePort,
    private val iniciarExecucao: IniciarExecucaoUseCase,
    private val finalizar: FinalizarOrdemServicoUseCase,
    private val cancelar: CancelarOrdemServicoUseCase,
    private val prazoDaEtapa: Duration,
) {

    private val log = LoggerFactory.getLogger(OrquestradorDaSaga::class.java)

    @Transactional
    open fun abrirReserva(ordemServicoId: OrdemServicoId): Saga = abrir(ordemServicoId, TipoDeSaga.RESERVA)

    @Transactional
    open fun abrirConsumo(ordemServicoId: OrdemServicoId): Saga = abrir(ordemServicoId, TipoDeSaga.CONSUMO)

    @Transactional
    open fun confirmarPasso(ordemServicoId: OrdemServicoId, insumoId: InsumoId): Boolean =
        aplicar(ordemServicoId) { it.confirmar(insumoId) }

    @Transactional
    open fun recusarPasso(ordemServicoId: OrdemServicoId, insumoId: InsumoId, motivo: String): Boolean =
        aplicar(ordemServicoId) { it.recusar(insumoId, motivo) }

    @Transactional
    open fun expirarPasso(ordemServicoId: OrdemServicoId, insumoId: InsumoId, motivo: String): Boolean {
        val abertas = sagas.emCursoPorOrdem(ordemServicoId)
        if (abertas.isEmpty()) {
            log.info(
                "reserva expirada sem saga aberta, ignorada ordemServicoId={} insumoId={}",
                ordemServicoId.valor, insumoId.valor,
            )
            return false
        }

        return aplicar(ordemServicoId) { it.expirar(insumoId, motivo) }
    }

    @Transactional
    open fun reprovarProximaVencida(): Boolean {
        val agora = LocalDateTime.now()
        val saga = sagas.travarProximaVencida(agora) ?: return false

        if (!saga.reprovarPorPrazo(agora)) return false

        log.warn(
            "prazo de etapa vencido ordemServicoId={} tipo={} etapa={}",
            saga.ordemServicoId.valor, saga.tipo, saga.etapa,
        )

        val salva = sagas.salvar(saga)
        publicar(salva)
        encaminhar(salva)
        return true
    }

    private fun abrir(ordemServicoId: OrdemServicoId, tipo: TipoDeSaga): Saga {
        sagas.porOrdemETipo(ordemServicoId, tipo)?.let { existente ->
            if (existente.reabrir(LocalDateTime.now().plus(prazoDaEtapa))) {
                val reaberta = sagas.salvar(existente)
                publicar(reaberta)
                log.warn(
                    "saga reaberta ordemServicoId={} tipo={} tentativa={} passos={}",
                    ordemServicoId.valor, tipo, reaberta.tentativa, reaberta.passosPendentes().size,
                )
                return reaberta
            }

            log.info(
                "saga ja existe, reaproveitada ordemServicoId={} tipo={} situacao={}",
                ordemServicoId.valor, tipo, existente.situacao,
            )
            return existente
        }

        val ordem = ordens.porId(ordemServicoId)
            ?: throw RecursoNaoEncontradoException("Ordem de serviço ${ordemServicoId.valor} não encontrada")

        val insumos = ordem.listarInsumos().associate { it.insumoId to it.quantidade }
        if (insumos.isEmpty()) {
            throw DomainException("Ordem de serviço sem insumo não precisa de saga de estoque")
        }

        val saga = sagas.salvar(
            Saga.abrir(
                ordemServicoId = ordemServicoId,
                tipo = tipo,
                insumos = insumos,
                prazoDaEtapa = LocalDateTime.now().plus(prazoDaEtapa),
            )
        )

        publicar(saga)
        log.info(
            "saga aberta ordemServicoId={} tipo={} etapa={} passos={}",
            ordemServicoId.valor, tipo, saga.etapa, saga.listarPassos().size,
        )
        return saga
    }

    private fun aplicar(ordemServicoId: OrdemServicoId, operacao: (Saga) -> Boolean): Boolean {
        val abertas = sagas.emCursoPorOrdem(ordemServicoId)
        if (abertas.isEmpty()) return false

        var mudou = false
        abertas.forEach { saga ->
            val antes = saga.etapa
            if (!operacao(saga)) return@forEach

            mudou = true
            val salva = sagas.salvar(saga)
            if (salva.etapa != antes) publicar(salva)
            encaminhar(salva)
        }
        return mudou
    }

    private fun encaminhar(saga: Saga) {
        when (saga.situacao) {
            SituacaoDaSaga.CONCLUIDA -> concluir(saga)
            SituacaoDaSaga.COMPENSADA -> cancelarPorCompensacao(saga)
            SituacaoDaSaga.FALHA -> log.error(
                "saga em falha sem compensacao possivel ordemServicoId={} tipo={} motivo={}",
                saga.ordemServicoId.valor, saga.tipo, saga.motivo,
            )

            else -> Unit
        }
    }

    private fun concluir(saga: Saga) {
        when (saga.tipo) {
            TipoDeSaga.RESERVA -> iniciarExecucao.executar(IniciarExecucaoCommand(saga.ordemServicoId))
            TipoDeSaga.CONSUMO -> finalizar.executar(FinalizarOrdemServicoCommand(saga.ordemServicoId))
        }
        log.info("saga concluida ordemServicoId={} tipo={}", saga.ordemServicoId.valor, saga.tipo)
    }

    private fun cancelarPorCompensacao(saga: Saga) {
        cancelar.executar(
            CancelarOrdemServicoCommand(
                ordemServicoId = saga.ordemServicoId,
                motivo = saga.motivo ?: "compensação da saga de ${saga.tipo.name.lowercase()}",
            )
        )
        log.warn(
            "saga compensada e ordem cancelada ordemServicoId={} motivo={}",
            saga.ordemServicoId.valor, saga.motivo,
        )
    }

    private fun publicar(saga: Saga) {
        val pendentes = saga.passosPendentes()
        if (pendentes.isEmpty()) return

        outbox.enfileirar(comandos.comandosDe(saga, pendentes))
    }
}
