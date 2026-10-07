package com.clau.service_track.ordens.application.port.out.repository

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId

interface HistoricoStatusRepositoryPort {

    fun registrar(transicao: TransicaoDeStatus)

    fun porOrdem(id: OrdemServicoId): List<TransicaoDeStatus>
}
