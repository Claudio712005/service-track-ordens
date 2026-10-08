package com.clau.service_track.ordens.infrastructure.adapter.`in`.mensageria

import com.clau.service_track.ordens.application.handler.saga.OrquestradorDaSaga
import com.clau.service_track.ordens.application.port.out.mensageria.RegistroDeMensagemPort
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.infrastructure.adapter.`in`.mensageria.dto.DadosDeEventoDeEstoque
import com.clau.service_track.ordens.infrastructure.adapter.`in`.mensageria.dto.EnvelopeDeMensagem
import com.clau.service_track.ordens.infrastructure.adapter.web.filter.CorrelacaoFilter
import java.util.UUID
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "servicetrack.messaging", name = ["enabled"], havingValue = "true")
class ConsumidorDeEventosDeEstoque(
    private val leitor: LeitorDeEnvelope,
    private val orquestrador: OrquestradorDaSaga,
    private val mensagens: RegistroDeMensagemPort,
) {

    private val log = LoggerFactory.getLogger(ConsumidorDeEventosDeEstoque::class.java)

    @KafkaListener(
        topics = ["\${servicetrack.messaging.stock-event-topic}"],
        containerFactory = "sagaListenerContainerFactory",
    )
    fun consumir(registro: ConsumerRecord<String, String>) {
        val envelope = leitor.ler(registro)
        anotarContexto(envelope)

        try {
            if (mensagens.jaProcessada(envelope.tipo, envelope.idMensagem)) {
                log.debug("mensagem ja processada tipo={} idMensagem={}", envelope.tipo, envelope.idMensagem)
                return
            }

            log.debug(
                "evento recebido tipo={} versao={} idMensagem={} particao={} offset={}",
                envelope.tipo, envelope.versao, envelope.idMensagem, registro.partition(), registro.offset(),
            )

            despachar(envelope)
            mensagens.registrarProcessada(envelope.tipo, envelope.idMensagem)
        } finally {
            MDC.remove(CorrelacaoFilter.CHAVE_CORRELACAO)
            MDC.remove(CorrelacaoFilter.CHAVE_REQUISICAO)
        }
    }

    private fun despachar(envelope: EnvelopeDeMensagem) {
        val dados = leitor.dados(envelope, DadosDeEventoDeEstoque::class.java)
        val ordem = OrdemServicoId.de(dados.ordemServicoId)
        val insumo = InsumoId.de(dados.insumoId)

        when (envelope.tipo) {
            ESTOQUE_RESERVADO, ESTOQUE_CONSUMIDO, RESERVA_LIBERADA -> orquestrador.confirmarPasso(ordem, insumo)

            RESERVA_RECUSADA, CONSUMO_RECUSADO ->
                orquestrador.recusarPasso(ordem, insumo, dados.motivo ?: envelope.tipo)

            RESERVA_EXPIRADA ->
                orquestrador.expirarPasso(ordem, insumo, dados.motivo ?: "reserva expirada no catalogo")

            else -> throw MensagemInvalidaException(
                "Evento '${envelope.tipo}' não é reconhecido por este serviço"
            )
        }
    }

    private fun anotarContexto(envelope: EnvelopeDeMensagem) {
        MDC.put(CorrelacaoFilter.CHAVE_CORRELACAO, envelope.correlationId ?: UUID.randomUUID().toString())
        MDC.put(CorrelacaoFilter.CHAVE_REQUISICAO, UUID.randomUUID().toString())
    }

    private companion object {
        const val ESTOQUE_RESERVADO = "EstoqueReservado"
        const val ESTOQUE_CONSUMIDO = "EstoqueConsumido"
        const val RESERVA_LIBERADA = "ReservaLiberada"
        const val RESERVA_RECUSADA = "ReservaRecusada"
        const val CONSUMO_RECUSADO = "ConsumoRecusado"
        const val RESERVA_EXPIRADA = "ReservaExpirada"
    }
}
