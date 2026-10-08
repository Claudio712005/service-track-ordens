package com.clau.service_track.ordens.infrastructure.adapter.config.mensageria

import com.clau.service_track.ordens.application.handler.saga.OrquestradorDaSaga
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.CancelarOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.FinalizarOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarExecucaoUseCase
import com.clau.service_track.ordens.application.port.out.mensageria.FabricaDeComandoDeEstoquePort
import com.clau.service_track.ordens.application.port.out.mensageria.OutboxPort
import com.clau.service_track.ordens.application.port.out.repository.OrdemServicoRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.SagaRepositoryPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SagaConfig {

    @Bean
    fun orquestradorDaSaga(
        sagas: SagaRepositoryPort,
        ordens: OrdemServicoRepositoryPort,
        outbox: OutboxPort,
        comandos: FabricaDeComandoDeEstoquePort,
        iniciarExecucao: IniciarExecucaoUseCase,
        finalizar: FinalizarOrdemServicoUseCase,
        cancelar: CancelarOrdemServicoUseCase,
        propriedades: MensageriaProperties,
    ) = OrquestradorDaSaga(
        sagas = sagas,
        ordens = ordens,
        outbox = outbox,
        comandos = comandos,
        iniciarExecucao = iniciarExecucao,
        finalizar = finalizar,
        cancelar = cancelar,
        prazoDaEtapa = propriedades.saga.stepDeadline,
    )
}
