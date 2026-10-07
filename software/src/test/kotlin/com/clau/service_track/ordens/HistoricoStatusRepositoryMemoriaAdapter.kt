package com.clau.service_track.ordens

import com.clau.service_track.ordens.application.port.out.repository.HistoricoStatusRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.TransicaoDeStatus
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import java.util.concurrent.CopyOnWriteArrayList

class HistoricoStatusRepositoryMemoriaAdapter : HistoricoStatusRepositoryPort {

    val transicoes = CopyOnWriteArrayList<TransicaoDeStatus>()

    fun reiniciar() = transicoes.clear()

    override fun registrar(transicao: TransicaoDeStatus) {
        transicoes.add(transicao)
    }

    override fun porOrdem(id: OrdemServicoId): List<TransicaoDeStatus> =
        transicoes.filter { it.ordemServicoId == id }.sortedBy { it.ocorridoEm }
}
