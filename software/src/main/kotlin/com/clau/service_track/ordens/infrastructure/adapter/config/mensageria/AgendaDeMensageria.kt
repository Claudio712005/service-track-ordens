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
        val vencidas = runCatching { orquestrador.comPrazoVencido(propriedades.saga.deadlineBatch) }
            .onFailure { log.warn("leitura de saga vencida falhou, sera retentada motivo={}", it.message) }
            .getOrDefault(emptyList())

        val reprovadas = vencidas.count { saga ->
            runCatching { orquestrador.reprovarPorPrazo(saga) }
                .onFailure {
                    log.warn(
                        "reprovacao por prazo falhou ordemServicoId={} motivo={}",
                        saga.ordemServicoId.valor, it.message,
                    )
                }
                .getOrDefault(false)
        }

        if (reprovadas > 0) log.info("sagas reprovadas por prazo quantidade={}", reprovadas)
    }
}
