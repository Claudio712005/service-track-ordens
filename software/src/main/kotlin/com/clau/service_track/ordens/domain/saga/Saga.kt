package com.clau.service_track.ordens.domain.saga

import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.saga.vo.SagaId
import java.math.BigDecimal
import java.time.LocalDateTime

class Saga private constructor(
    val id: SagaId,
    val ordemServicoId: OrdemServicoId,
    val tipo: TipoDeSaga,
    situacao: SituacaoDaSaga,
    etapa: EtapaDaSaga,
    prazoDaEtapa: LocalDateTime,
    motivo: String?,
    private val passos: MutableList<PassoDaSaga>,
    val dataCriacao: LocalDateTime,
    dataAtualizacao: LocalDateTime,
) {

    var situacao: SituacaoDaSaga = situacao
        private set

    var etapa: EtapaDaSaga = etapa
        private set

    var prazoDaEtapa: LocalDateTime = prazoDaEtapa
        private set

    var motivo: String? = motivo
        private set

    var dataAtualizacao: LocalDateTime = dataAtualizacao
        private set

    companion object {

        const val PRAZO_VENCIDO = "prazo da etapa vencido sem resposta de todos os passos"
        const val MINUTOS_DA_COMPENSACAO = 2L

        fun abrir(
            ordemServicoId: OrdemServicoId,
            tipo: TipoDeSaga,
            insumos: Map<InsumoId, BigDecimal>,
            prazoDaEtapa: LocalDateTime,
        ): Saga {
            if (insumos.isEmpty()) {
                throw DomainException("Saga sem insumo não tem passo, e saga sem passo não tem o que orquestrar")
            }

            val agora = LocalDateTime.now()
            return Saga(
                id = SagaId.gerar(),
                ordemServicoId = ordemServicoId,
                tipo = tipo,
                situacao = SituacaoDaSaga.EM_CURSO,
                etapa = tipo.etapaInicial,
                prazoDaEtapa = prazoDaEtapa,
                motivo = null,
                passos = insumos
                    .map { (insumoId, quantidade) -> PassoDaSaga.pedir(tipo.etapaInicial, insumoId, quantidade) }
                    .toMutableList(),
                dataCriacao = agora,
                dataAtualizacao = agora,
            )
        }

        fun reconstituir(
            id: SagaId,
            ordemServicoId: OrdemServicoId,
            tipo: TipoDeSaga,
            situacao: SituacaoDaSaga,
            etapa: EtapaDaSaga,
            prazoDaEtapa: LocalDateTime,
            motivo: String?,
            passos: MutableList<PassoDaSaga>,
            dataCriacao: LocalDateTime,
            dataAtualizacao: LocalDateTime,
        ) = Saga(id, ordemServicoId, tipo, situacao, etapa, prazoDaEtapa, motivo, passos, dataCriacao, dataAtualizacao)
    }

    fun listarPassos(): List<PassoDaSaga> = passos.toList()

    fun passosDaEtapa(etapaPedida: EtapaDaSaga): List<PassoDaSaga> = passos.filter { it.etapa == etapaPedida }

    fun passosPendentes(): List<PassoDaSaga> = passos.filter { it.etapa == etapa && it.pendente }

    fun expirouEm(momento: LocalDateTime): Boolean =
        !situacao.encerrada && momento.isAfter(prazoDaEtapa)

    fun confirmar(insumoId: InsumoId): Boolean {
        val passo = passoCorrente(insumoId) ?: return false
        if (!passo.resolver(situacaoDeSucesso())) return false

        avancarSeEtapaConcluida()
        marcarAtualizacao()
        return true
    }

    fun recusar(insumoId: InsumoId, motivoDaRecusa: String): Boolean =
        reprovar(insumoId, SituacaoDoPasso.RECUSADO, motivoDaRecusa)

    fun expirar(insumoId: InsumoId, motivoDaExpiracao: String): Boolean =
        reprovar(insumoId, SituacaoDoPasso.EXPIRADO, motivoDaExpiracao)

    fun reprovarPorPrazo(agora: LocalDateTime): Boolean {
        if (situacao.encerrada) return false

        val pendentes = passosPendentes()
        if (pendentes.isEmpty()) return false

        pendentes.forEach { it.resolver(SituacaoDoPasso.EXPIRADO, PRAZO_VENCIDO) }
        encaminharReprovacao(PRAZO_VENCIDO, agora)
        return true
    }

    fun insumosAcompensar(): Map<InsumoId, BigDecimal> = passos
        .filter { it.etapa == tipo.etapaInicial && it.situacao == SituacaoDoPasso.CONFIRMADO }
        .associate { it.insumoId to it.quantidade }

    private fun reprovar(insumoId: InsumoId, resultado: SituacaoDoPasso, motivoDaReprovacao: String): Boolean {
        val passo = passoCorrente(insumoId) ?: return false
        if (!passo.resolver(resultado, motivoDaReprovacao)) return false

        encaminharReprovacao(motivoDaReprovacao, LocalDateTime.now())
        return true
    }

    private fun encaminharReprovacao(motivoDaReprovacao: String, agora: LocalDateTime) {
        motivo = motivoDaReprovacao

        if (!tipo.compensavel) {
            situacao = SituacaoDaSaga.FALHA
            marcarAtualizacao()
            return
        }

        if (situacao == SituacaoDaSaga.COMPENSANDO) {
            marcarAtualizacao()
            return
        }

        val aCompensar = insumosAcompensar()
        passosPendentes().forEach { it.resolver(SituacaoDoPasso.EXPIRADO, motivoDaReprovacao) }

        if (aCompensar.isEmpty()) {
            situacao = SituacaoDaSaga.COMPENSADA
            marcarAtualizacao()
            return
        }

        situacao = SituacaoDaSaga.COMPENSANDO
        etapa = EtapaDaSaga.LIBERACAO_DE_INSUMOS
        prazoDaEtapa = agora.plusMinutes(MINUTOS_DA_COMPENSACAO)
        aCompensar.forEach { (insumoId, quantidade) ->
            passos += PassoDaSaga.pedir(EtapaDaSaga.LIBERACAO_DE_INSUMOS, insumoId, quantidade)
        }
        marcarAtualizacao()
    }

    private fun avancarSeEtapaConcluida() {
        if (passosPendentes().isNotEmpty()) return

        situacao = when (situacao) {
            SituacaoDaSaga.COMPENSANDO -> SituacaoDaSaga.COMPENSADA
            else -> SituacaoDaSaga.CONCLUIDA
        }
    }

    private fun situacaoDeSucesso(): SituacaoDoPasso =
        if (situacao == SituacaoDaSaga.COMPENSANDO) SituacaoDoPasso.COMPENSADO else SituacaoDoPasso.CONFIRMADO

    private fun passoCorrente(insumoId: InsumoId): PassoDaSaga? =
        passos.firstOrNull { it.etapa == etapa && it.insumoId == insumoId }

    private fun marcarAtualizacao() {
        dataAtualizacao = LocalDateTime.now()
    }
}
