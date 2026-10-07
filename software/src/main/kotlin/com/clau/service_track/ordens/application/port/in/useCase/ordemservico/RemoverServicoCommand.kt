package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.ServicoId

data class RemoverServicoCommand(
    val ordemServicoId: OrdemServicoId,
    val servicoId: ServicoId,
)
