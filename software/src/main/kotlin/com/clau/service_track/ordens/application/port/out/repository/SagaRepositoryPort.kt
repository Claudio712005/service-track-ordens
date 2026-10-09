package com.clau.service_track.ordens.application.port.out.repository

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.saga.Saga
import com.clau.service_track.ordens.domain.saga.TipoDeSaga
import java.time.LocalDateTime

interface SagaRepositoryPort {

    fun salvar(saga: Saga): Saga

    fun porOrdemETipo(ordemServicoId: OrdemServicoId, tipo: TipoDeSaga): Saga?

    fun emCursoPorOrdem(ordemServicoId: OrdemServicoId): List<Saga>

    fun travarProximaVencida(momento: LocalDateTime): Saga?
}
