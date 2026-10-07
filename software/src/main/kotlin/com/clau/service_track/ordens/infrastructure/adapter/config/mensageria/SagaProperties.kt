package com.clau.service_track.ordens.infrastructure.adapter.config.mensageria

import java.time.Duration

data class SagaProperties(

    val stepDeadline: Duration = Duration.ofMinutes(2),

    val reservationDeadline: Duration = Duration.ofMinutes(10),

    val deadlineSweepInterval: Duration = Duration.ofSeconds(30),

    val deadlineBatch: Int = 50,
)
