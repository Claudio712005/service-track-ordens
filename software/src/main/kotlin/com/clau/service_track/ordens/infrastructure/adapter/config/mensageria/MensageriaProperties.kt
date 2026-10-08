package com.clau.service_track.ordens.infrastructure.adapter.config.mensageria

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "servicetrack.messaging")
data class MensageriaProperties(

    val enabled: Boolean = false,

    val group: String = "ordens-saga",

    val stockCommandTopic: String = "servicetrack.estoque.comandos.v1",

    val stockEventTopic: String = "servicetrack.estoque.eventos.v1",

    val dltSuffix: String = ".dlt",

    val concurrency: Int = 1,

    val attempts: Int = 4,

    val initialBackoff: Duration = Duration.ofMillis(500),

    val maxBackoff: Duration = Duration.ofSeconds(5),

    val backoffMultiplier: Double = 2.0,

    val publishInterval: Duration = Duration.ofSeconds(2),

    val publishBatch: Int = 50,

    val saga: SagaProperties = SagaProperties(),
) {

    val dltTopic: String
        get() = stockEventTopic + dltSuffix
}
