package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.ItemOrdemServicoId
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId

data class ConcluirItemServicoCommand(
    val ordemServicoId: OrdemServicoId,
    val itemId: ItemOrdemServicoId,
    val mecanicoId: UsuarioId,
    val observacao: String,
)
