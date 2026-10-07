package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId

data class CancelarOrdemServicoCommand(
    val ordemServicoId: OrdemServicoId,
    val motivo: String? = null,
)
