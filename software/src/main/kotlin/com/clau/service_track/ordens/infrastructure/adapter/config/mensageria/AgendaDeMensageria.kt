package com.clau.service_track.ordens.infrastructure.adapter.config.mensageria

import com.clau.service_track.ordens.application.handler.saga.OrquestradorDaSaga
import com.clau.service_track.ordens.infrastructure.adapter.out.mensageria.PublicadorDeOutbox
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "servicetrack.messaging", name = ["enabled"], havingValue = "true")
class AgendaDeMensageria(
    private val publicador: PublicadorDeOutbox,
    private val orquestrador: OrquestradorDaSaga,
    private val propriedades: MensageriaProperties,
) {

    private val log = LoggerFactory.getLogger(AgendaDeMensageria::class.java)

    @Scheduled(fixedDelayString = "\${servicetrack.messaging.publish-interval:2s}")
    fun publicarPendentes() {
        runCatching { publicador.publicarPendentes() }
            .onFailure { log.warn("publicacao do outbox falhou, sera retentada motivo={}", it.message) }
    }

    @Scheduled(fixedDelayString = "\${servicetrack.messaging.saga.deadline-sweep-interval:30s}")
    fun varrerPrazos() {
        var reprovadas = 0

        while (reprovadas < propriedades.saga.deadlineBatch) {
            val reprovou = runCatching { orquestrador.reprovarProximaVencida() }
                .onFailure { log.warn("reprovacao por prazo falhou, sera retentada motivo={}", it.message) }
                .getOrDefault(false)

            if (!reprovou) break
            reprovadas++
        }

        if (reprovadas > 0) log.info("sagas reprovadas por prazo quantidade={}", reprovadas)
    }
}
