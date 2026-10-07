package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import java.time.LocalDateTime

data class AbrirOrdemServicoCommand(
    val motivo: String,
    val clienteId: UsuarioId,
    val mecanicoId: UsuarioId,
    val veiculoId: VeiculoId,
    val observacao: String = "",
    val prazoConclusao: LocalDateTime? = null,
)
