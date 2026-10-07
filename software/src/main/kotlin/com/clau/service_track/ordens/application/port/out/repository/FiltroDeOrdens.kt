package com.clau.service_track.ordens.application.port.out.repository

import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId

data class FiltroDeOrdens(
    val clienteId: UsuarioId? = null,
    val veiculoId: VeiculoId? = null,
    val mecanicoId: UsuarioId? = null,
    val status: StatusOrdemServicoEnum? = null,
)
