package com.clau.service_track.ordens.infrastructure.adapter.config.mensageria

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.clau.service_track.ordens.infrastructure.adapter.`in`.mensageria.ConsumidorDeEventosDeEstoque
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.test.context.EmbeddedKafka

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:st_ord_trace;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS ORDENS",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "servicetrack.messaging.enabled=true",
        "servicetrack.messaging.group=ordens-trace-teste",
        "servicetrack.messaging.publish-interval=1h",
        "servicetrack.messaging.saga.deadline-sweep-interval=1h",
        "management.tracing.sampling.probability=1.0",
    ]
)
@AutoConfigureTracing
@EmbeddedKafka(
    partitions = 1,
    topics = ["servicetrack.estoque.comandos.v1", "servicetrack.estoque.eventos.v1"],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
class ConsumidorEntraNoTraceDaFilaTest {

    @Autowired
    @Qualifier("sagaListenerContainerFactory")
    private lateinit var fabrica: ConcurrentKafkaListenerContainerFactory<String, String>

    @Autowired
    private lateinit var produtores: ProducerFactory<String, String>

    private val coletor = ListAppender<ILoggingEvent>()
    private lateinit var logger: Logger

    @BeforeTest
    fun ligarColetor() {
        logger = LoggerFactory.getLogger(ConsumidorDeEventosDeEstoque::class.java) as Logger
        coletor.start()
        logger.addAppender(coletor)
        logger.level = Level.DEBUG
    }

    @AfterTest
    fun desligarColetor() {
        logger.detachAppender(coletor)
        coletor.stop()
    }

    @Test
    fun `a fabrica da saga observa, senao o traceparent recebido nao viraria span`() {
        assertTrue(
            fabrica.containerProperties.isObservationEnabled,
            "spring.kafka.listener.observation-enabled so alcanca a fabrica auto-configurada do Boot; " +
                "fabrica declarada como @Bean precisa ligar observacao a mao",
        )
    }

    @Test
    fun `o consumidor da saga adota o traceparent que veio na mensagem`() {
        val traceDeQuemPublicou = "4bf92f3577b34da6a3ce929d0e0e4736"
        val idMensagem = UUID.randomUUID()

        publicar(idMensagem, "00-$traceDeQuemPublicou-00f067aa0ba902b7-01")

        val traceNoConsumo = esperarTraceDoConsumo(idMensagem)

        assertEquals(
            traceDeQuemPublicou,
            traceNoConsumo,
            "o consumidor abriu trace proprio: o rastro morre na fila e o orquestrador " +
                "nao aparece no trace de quem pediu a reserva",
        )
    }

    private fun publicar(idMensagem: UUID, traceparent: String) {
        val payload = """
            {"idMensagem":"$idMensagem","tipo":"EstoqueReservado","versao":1,
             "ocorridoEm":"${OffsetDateTime.now(ZoneOffset.UTC)}","correlationId":"trace-1",
             "dados":{"insumoId":"${UUID.randomUUID()}","ordemServicoId":"${UUID.randomUUID()}","sku":"OLEO-5W30"}}
        """.trimIndent().replace("\n", "")

        val registro = ProducerRecord(TOPICO_DE_EVENTOS, idMensagem.toString(), payload)
        registro.headers().add(RecordHeader("traceparent", traceparent.toByteArray()))

        val semObservacao = KafkaTemplate(produtores).apply { setObservationEnabled(false) }
        semObservacao.send(registro).get(ESPERA_DE_ENVIO, TimeUnit.SECONDS)
    }

    private fun esperarTraceDoConsumo(idMensagem: UUID): String? {
        val limite = System.nanoTime() + Duration.ofSeconds(ESPERA_DE_CONSUMO).toNanos()
        while (System.nanoTime() < limite) {
            val evento = coletor.list.firstOrNull {
                it.formattedMessage.contains("evento recebido") &&
                    it.formattedMessage.contains(idMensagem.toString())
            }
            if (evento != null) {
                return assertNotNull(
                    evento.mdcPropertyMap["traceId"],
                    "sem traceId no MDC durante o consumo nao ha span nenhum: ${evento.mdcPropertyMap}",
                )
            }
            Thread.sleep(50)
        }
        throw AssertionError("a mensagem $idMensagem nao foi consumida em ${ESPERA_DE_CONSUMO}s")
    }

    private companion object {
        const val TOPICO_DE_EVENTOS = "servicetrack.estoque.eventos.v1"
        const val ESPERA_DE_ENVIO = 10L
        const val ESPERA_DE_CONSUMO = 30L
    }
}
