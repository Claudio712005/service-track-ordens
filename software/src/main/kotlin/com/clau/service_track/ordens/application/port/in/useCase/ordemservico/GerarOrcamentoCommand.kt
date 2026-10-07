package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.vo.ValorMonetario

data class GerarOrcamentoCommand(
    val ordemServicoId: OrdemServicoId,
    val custoMaoDeObra: ValorMonetario,
    val custoInsumos: ValorMonetario,
)
