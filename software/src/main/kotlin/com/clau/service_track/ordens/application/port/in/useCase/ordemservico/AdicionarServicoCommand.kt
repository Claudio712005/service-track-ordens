package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.ServicoId
import com.clau.service_track.ordens.domain.vo.ValorMonetario

data class AdicionarServicoCommand(
    val ordemServicoId: OrdemServicoId,
    val servicoId: ServicoId,
    val valor: ValorMonetario,
)
