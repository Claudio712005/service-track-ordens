package com.clau.service_track.ordens

import com.clau.service_track.ordens.application.port.out.repository.SagaRepositoryPort
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.saga.Saga
import com.clau.service_track.ordens.domain.saga.TipoDeSaga
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class SagaRepositoryMemoriaAdapter : SagaRepositoryPort {

    private val sagas = ConcurrentHashMap<String, Saga>()
    private var relogio: LocalDateTime? = null

    fun reiniciar() {
        sagas.clear()
        relogio = null
    }

    fun vencerPrazos() {
        relogio = LocalDateTime.now().plusHours(1)
    }

    fun restaurarRelogio() {
        relogio = null
    }

    override fun salvar(saga: Saga): Saga {
        sagas[saga.id.valor] = saga
        return saga
    }

    override fun porOrdemETipo(ordemServicoId: OrdemServicoId, tipo: TipoDeSaga): Saga? = sagas.values
        .firstOrNull { it.ordemServicoId == ordemServicoId && it.tipo == tipo }

    override fun emCursoPorOrdem(ordemServicoId: OrdemServicoId): List<Saga> = sagas.values
        .filter { it.ordemServicoId == ordemServicoId && !it.situacao.encerrada }

    override fun travarProximaVencida(momento: LocalDateTime): Saga? {
        val referencia = relogio ?: momento
        return sagas.values.firstOrNull { it.expirouEm(referencia) }
    }
}
