package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId

data class ReatribuirMecanicoCommand(
    val ordemServicoId: OrdemServicoId,
    val mecanicoId: UsuarioId,
)
