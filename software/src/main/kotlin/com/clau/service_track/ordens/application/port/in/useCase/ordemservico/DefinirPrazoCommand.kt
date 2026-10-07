package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import java.time.LocalDateTime

data class DefinirPrazoCommand(
    val ordemServicoId: OrdemServicoId,
    val prazoConclusao: LocalDateTime,
)
