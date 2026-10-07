package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId

fun interface ConsultarOrdemServicoUseCase {

    fun executar(id: OrdemServicoId): OrdemServico
}
