package com.clau.service_track.ordens.application.handler.ordemservico

import com.clau.service_track.ordens.application.handler.saga.OrquestradorDaSaga
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AprovarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AprovarOrcamentoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.FinalizarOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.FinalizarOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarExecucaoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarExecucaoUseCase
import com.clau.service_track.ordens.application.port.out.repository.OrdemServicoRepositoryPort
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import org.slf4j.LoggerFactory

open class FluxoDaOrdemServico(
    private val ordens: OrdemServicoRepositoryPort,
    private val aprovar: AprovarOrcamentoUseCase,
    private val iniciarExecucao: IniciarExecucaoUseCase,
    private val finalizar: FinalizarOrdemServicoUseCase,
    private val orquestrador: OrquestradorDaSaga,
) {

    private val log = LoggerFactory.getLogger(FluxoDaOrdemServico::class.java)

    open fun aprovarOrcamento(id: OrdemServicoId): OrdemServico {
        val aprovada = aprovar.executar(AprovarOrcamentoCommand(id))

        if (semInsumo(aprovada)) {
            log.info("ordem sem insumo dispensa saga de reserva ordemServicoId={}", id.valor)
            return iniciarExecucao.executar(IniciarExecucaoCommand(id))
        }

        orquestrador.abrirReserva(id)
        return ordens.porId(id) ?: aprovada
    }

    open fun pedirFinalizacao(id: OrdemServicoId): OrdemServico {
        val ordem = ordens.porId(id)

        if (ordem != null && semInsumo(ordem)) {
            log.info("ordem sem insumo dispensa saga de consumo ordemServicoId={}", id.valor)
            return finalizar.executar(FinalizarOrdemServicoCommand(id))
        }

        orquestrador.abrirConsumo(id)
        return ordens.porId(id) ?: ordem ?: finalizar.executar(FinalizarOrdemServicoCommand(id))
    }

    private fun semInsumo(ordem: OrdemServico): Boolean = ordem.listarInsumos().isEmpty()
}
