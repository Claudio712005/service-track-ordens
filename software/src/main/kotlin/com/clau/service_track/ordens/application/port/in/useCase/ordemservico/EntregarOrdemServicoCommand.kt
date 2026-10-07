package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId

data class EntregarOrdemServicoCommand(
    val ordemServicoId: OrdemServicoId,
)
