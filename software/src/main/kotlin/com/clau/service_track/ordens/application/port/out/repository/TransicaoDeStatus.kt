package com.clau.service_track.ordens.application.port.out.repository

import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import java.time.OffsetDateTime

data class TransicaoDeStatus(
    val ordemServicoId: OrdemServicoId,
    val statusAnterior: StatusOrdemServicoEnum?,
    val statusNovo: StatusOrdemServicoEnum,
    val motivo: String?,
    val correlationId: String?,
    val ocorridoEm: OffsetDateTime,
)
