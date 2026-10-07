package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import java.math.BigDecimal

data class AdicionarInsumoCommand(
    val ordemServicoId: OrdemServicoId,
    val insumoId: InsumoId,
    val quantidade: BigDecimal,
)
