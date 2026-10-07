package com.clau.service_track.ordens.domain.ordemservico.vo

import java.time.Duration
import java.time.LocalDateTime

@JvmInline
value class PrazoConclusao(val valor: LocalDateTime) {

    fun horasRestantes(agora: LocalDateTime): Long {
        return Duration.between(agora, valor).toHours()
    }
}
