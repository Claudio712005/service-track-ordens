package com.clau.service_track.ordens.infrastructure.adapter.out.mensageria

import com.clau.service_track.ordens.infrastructure.adapter.config.mensageria.MensageriaProperties
import com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres.OutboxJpaRepository
import com.clau.service_track.ordens.infrastructure.entity.postgres.OutboxEntity
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Limit
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ConditionalOnProperty(prefix = "servicetrack.messaging", name = ["enabled"], havingValue = "true")
class PublicadorDeOutbox(
    private val outbox: OutboxJpaRepository,
    private val template: KafkaTemplate<String, String>,
    private val propriedades: MensageriaProperties,
) {

    private val log = LoggerFactory.getLogger(PublicadorDeOutbox::class.java)

    @Transactional
    fun publicarPendentes(): Int {
        val pendentes = outbox.reservarPendentes(Limit.of(propriedades.publishBatch))
        if (pendentes.isEmpty()) return 0

        pendentes.forEach(::enviar)
        log.info("mensagens publicadas quantidade={}", pendentes.size)
        return pendentes.size
    }

    private fun enviar(linha: OutboxEntity) {
        val registro = ProducerRecord<String, String>(linha.topico, linha.chaveParticao, linha.payload)
        registro.headers().add(cabecalho(CABECALHO_TIPO, linha.tipoMensagem))
        registro.headers().add(cabecalho(CABECALHO_VERSAO, linha.versaoMensagem.toString()))

        noSpanDeOrigem(linha.traceparent) {
            template.send(registro).get(ESPERA_DO_ENVIO_EM_SEGUNDOS, TimeUnit.SECONDS)
        }

        linha.dataPublicacao = OffsetDateTime.now(ZoneOffset.UTC)
    }

    private fun noSpanDeOrigem(traceparent: String?, envio: () -> Unit) {
        val contexto = contextoDe(traceparent)
        if (contexto == null) {
            envio()
            return
        }

        contexto.makeCurrent().use { envio() }
    }

    private fun contextoDe(traceparent: String?): Context? {
        val partes = traceparent?.split('-') ?: return null
        if (partes.size < 4) return null

        val contextoDoSpan = runCatching {
            SpanContext.createFromRemoteParent(
                partes[1],
                partes[2],
                TraceFlags.fromHex(partes[3], 0),
                TraceState.getDefault(),
            )
        }.getOrNull() ?: return null

        if (!contextoDoSpan.isValid) return null

        return Context.current().with(Span.wrap(contextoDoSpan))
    }

    private fun cabecalho(nome: String, valor: String) =
        RecordHeader(nome, valor.toByteArray(StandardCharsets.UTF_8))

    private companion object {
        const val CABECALHO_TIPO = "X-Tipo-Mensagem"
        const val CABECALHO_VERSAO = "X-Versao-Mensagem"
        const val ESPERA_DO_ENVIO_EM_SEGUNDOS = 15L
    }
}
