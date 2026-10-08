package com.clau.service_track.ordens.infrastructure.adapter.config.mensageria

import com.clau.service_track.ordens.application.handler.ordemservico.OrdemServicoCommandHandler
import com.clau.service_track.ordens.application.handler.saga.OrquestradorDaSaga
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AbrirOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AdicionarInsumoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AprovarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.GerarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarDiagnosticoCommand
import com.clau.service_track.ordens.application.port.out.repository.OrdemServicoRepositoryPort
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import com.clau.service_track.ordens.infrastructure.adapter.out.mensageria.PublicadorDeOutbox
import io.micrometer.tracing.Tracer
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.Awaitility.await
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:st_ord_saga;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS ORDENS",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "servicetrack.messaging.enabled=true",
        "servicetrack.messaging.group=ordens-saga-teste",
        "servicetrack.messaging.publish-interval=50ms",
        "servicetrack.messaging.saga.deadline-sweep-interval=1h",
    ]
)
@AutoConfigureTracing
@EmbeddedKafka(
    partitions = 1,
    topics = ["servicetrack.estoque.comandos.v1", "servicetrack.estoque.eventos.v1"],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
class SagaAtravessaAFilaTest {

    @Autowired
    private lateinit var escrita: OrdemServicoCommandHandler

    @Autowired
    private lateinit var orquestrador: OrquestradorDaSaga

    @Autowired
    private lateinit var publicador: PublicadorDeOutbox

    @Autowired
    private lateinit var ordens: OrdemServicoRepositoryPort

    @Autowired
    private lateinit var tracer: Tracer

    @Autowired
    private lateinit var template: KafkaTemplate<String, String>

    @Autowired
    private lateinit var broker: org.springframework.kafka.test.EmbeddedKafkaBroker

    private val oleo = InsumoId.gerar()
    private val filtro = InsumoId.gerar()

    private fun ordemAguardandoAprovacao(insumos: List<InsumoId>): OrdemServico {
        val ordem = escrita.executar(
            AbrirOrdemServicoCommand(
                motivo = "barulho na suspensao",
                clienteId = UsuarioId.gerar(),
                mecanicoId = UsuarioId.gerar(),
                veiculoId = VeiculoId.gerar(),
            )
        )
        escrita.executar(IniciarDiagnosticoCommand(ordem.id))
        insumos.forEach { escrita.executar(AdicionarInsumoCommand(ordem.id, it, BigDecimal("2"))) }
        escrita.executar(
            GerarOrcamentoCommand(ordem.id, ValorMonetario(BigDecimal("300")), ValorMonetario(BigDecimal("180")))
        )
        return escrita.executar(AprovarOrcamentoCommand(ordem.id))
    }

    private fun consumidorDeComandos(): KafkaConsumer<String, String> {
        val propriedades = KafkaTestUtils.consumerProps(
            broker.brokersAsString,
            "verificador-${UUID.randomUUID()}",
            "true",
        )
        val consumidor = KafkaConsumer(propriedades, StringDeserializer(), StringDeserializer())
        consumidor.subscribe(listOf(TOPICO_DE_COMANDOS))
        return consumidor
    }

    private fun cabecalho(registro: ConsumerRecord<String, String>, nome: String): String? = registro.headers()
        .lastHeader(nome)
        ?.value()
        ?.toString(StandardCharsets.UTF_8)

    private fun traceIdDe(traceparent: String?): String? = traceparent?.split('-')?.getOrNull(1)

    private fun publicarEvento(tipo: String, ordem: OrdemServicoId, insumo: InsumoId, motivo: String? = null) {
        val idMensagem = UUID.randomUUID()
        val extra = motivo?.let { ""","motivo":"$it"""" } ?: ""
        val payload = """
            {"idMensagem":"$idMensagem","tipo":"$tipo","versao":1,
             "ocorridoEm":"${OffsetDateTime.now(ZoneOffset.UTC)}","correlationId":"atendimento-1",
             "dados":{"insumoId":"${insumo.valor}","ordemServicoId":"${ordem.valor}","sku":"OLEO-5W30"$extra}}
        """.trimIndent().replace("\n", "")

        template.send(TOPICO_DE_EVENTOS, ordem.valor, payload).get(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    @Test
    fun `comando publicado pela outbox carrega o traceparent do span que abriu a saga`() {
        val consumidor = consumidorDeComandos()
        consumidor.use {
            KafkaTestUtils.getRecords(it, Duration.ofMillis(300))

            val span = tracer.nextSpan().name("abertura-da-saga").start()
            val traceDeOrigem = span.context().traceId()
            val ordem = tracer.withSpan(span).use {
                val aberta = ordemAguardandoAprovacao(listOf(oleo))
                orquestrador.abrirReserva(aberta.id)
                aberta
            }
            span.end()

            assertEquals(1, publicador.publicarPendentes())

            val registros = KafkaTestUtils.getRecords(consumidor, Duration.ofSeconds(10), 1)
            val comando = registros.records(TOPICO_DE_COMANDOS).single()

            assertEquals(ordem.id.valor, comando.key())
            assertEquals("ReservarEstoque", cabecalho(comando, "X-Tipo-Mensagem"))

            val traceparent = assertNotNull(
                cabecalho(comando, "traceparent"),
                "sem traceparent no cabecalho o trace nao atravessa a fila",
            )
            assertEquals(
                traceDeOrigem,
                traceIdDe(traceparent),
                "a outbox publica fora do span de origem; sem restaurar o traceparent guardado o " +
                    "consumidor entra num trace vizinho",
            )

            assertTrue(comando.value().contains(ordem.id.valor))
            assertTrue(comando.value().contains("expiraEm"))
        }
    }

    @Test
    fun `reserva confirmada pelos eventos leva a OS para execucao`() {
        val ordem = ordemAguardandoAprovacao(listOf(oleo, filtro))
        orquestrador.abrirReserva(ordem.id)
        publicador.publicarPendentes()

        publicarEvento("EstoqueReservado", ordem.id, oleo)
        publicarEvento("EstoqueReservado", ordem.id, filtro)

        await().atMost(Duration.ofSeconds(20)).untilAsserted {
            assertEquals(StatusOrdemServicoEnum.EM_EXECUCAO, ordens.porId(ordem.id)!!.obterStatus())
        }
    }

    @Test
    fun `recusa de reserva compensa o reservado e cancela a OS`() {
        val ordem = ordemAguardandoAprovacao(listOf(oleo, filtro))
        orquestrador.abrirReserva(ordem.id)
        publicador.publicarPendentes()

        publicarEvento("EstoqueReservado", ordem.id, oleo)
        publicarEvento("ReservaRecusada", ordem.id, filtro, "solicitado 2 LITRO, disponivel 0 LITRO")

        val consumidor = consumidorDeComandos()
        consumidor.use {
            await().atMost(Duration.ofSeconds(20)).untilAsserted {
                assertTrue(publicador.publicarPendentes() >= 0)
                val liberacao = KafkaTestUtils.getRecords(consumidor, Duration.ofMillis(500))
                    .records(TOPICO_DE_COMANDOS)
                    .any { registro -> cabecalho(registro, "X-Tipo-Mensagem") == "LiberarReserva" }
                assertTrue(liberacao, "a compensacao tem de publicar LiberarReserva para o insumo reservado")
            }
        }

        publicarEvento("ReservaLiberada", ordem.id, oleo)

        await().atMost(Duration.ofSeconds(20)).untilAsserted {
            assertEquals(StatusOrdemServicoEnum.CANCELADA, ordens.porId(ordem.id)!!.obterStatus())
        }
    }

    private companion object {
        const val TOPICO_DE_COMANDOS = "servicetrack.estoque.comandos.v1"
        const val TOPICO_DE_EVENTOS = "servicetrack.estoque.eventos.v1"
    }
}
