package com.clau.service_track.ordens.infrastructure.adapter.out.mensageria

import com.clau.service_track.ordens.application.port.out.mensageria.FabricaDeComandoDeEstoquePort
import com.clau.service_track.ordens.application.port.out.mensageria.MensagemParaPublicar
import com.clau.service_track.ordens.domain.saga.EtapaDaSaga
import com.clau.service_track.ordens.domain.saga.PassoDaSaga
import com.clau.service_track.ordens.domain.saga.Saga
import com.clau.service_track.ordens.infrastructure.adapter.config.mensageria.MensageriaProperties
import com.clau.service_track.ordens.infrastructure.adapter.out.mensageria.dto.DadosDeReservaEmAndamento
import com.clau.service_track.ordens.infrastructure.adapter.out.mensageria.dto.DadosDeReservarEstoque
import com.clau.service_track.ordens.infrastructure.adapter.out.mensageria.dto.EnvelopeDeSaida
import com.clau.service_track.ordens.infrastructure.adapter.web.filter.CorrelacaoFilter
import io.opentelemetry.api.trace.Span
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.slf4j.MDC
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class FabricaDeComandoDeEstoqueJson(
    private val mapper: ObjectMapper,
    private val propriedades: MensageriaProperties,
) : FabricaDeComandoDeEstoquePort {

    override fun comandosDe(saga: Saga, passos: List<PassoDaSaga>): List<MensagemParaPublicar> =
        passos.map { passo -> comando(saga, passo) }

    private fun comando(saga: Saga, passo: PassoDaSaga): MensagemParaPublicar {
        val tipo = tipoDe(passo.etapa)
        val idMensagem =
            "${saga.ordemServicoId.valor}:${passo.etapa.name}:${passo.insumoId.valor}:${saga.tentativa}"

        val dados: Any = if (passo.etapa == EtapaDaSaga.RESERVA_DE_INSUMOS) {
            DadosDeReservarEstoque(
                insumoId = passo.insumoId.valor,
                ordemServicoId = saga.ordemServicoId.valor,
                quantidade = passo.quantidade,
                expiraEm = prazoDaReserva(saga),
            )
        } else {
            DadosDeReservaEmAndamento(
                insumoId = passo.insumoId.valor,
                ordemServicoId = saga.ordemServicoId.valor,
            )
        }

        val envelope = EnvelopeDeSaida(
            idMensagem = idMensagem,
            tipo = tipo,
            versao = VERSAO_DO_CONTRATO,
            ocorridoEm = OffsetDateTime.now(ZoneOffset.UTC),
            correlationId = MDC.get(CorrelacaoFilter.CHAVE_CORRELACAO),
            traceId = traceAtual()?.let { traceIdDe(it) },
            dados = dados,
        )

        return MensagemParaPublicar(
            idMensagem = idMensagem,
            agregadoTipo = AGREGADO,
            agregadoId = saga.ordemServicoId.valor,
            chaveDeParticao = saga.ordemServicoId.valor,
            tipoMensagem = tipo,
            versaoMensagem = VERSAO_DO_CONTRATO,
            topico = propriedades.stockCommandTopic,
            payload = mapper.writeValueAsString(envelope),
            traceparent = traceAtual(),
        )
    }

    private fun prazoDaReserva(saga: Saga): OffsetDateTime = saga.dataCriacao
        .plus(propriedades.saga.reservationDeadline)
        .atZone(ZoneId.systemDefault())
        .toOffsetDateTime()
        .withOffsetSameInstant(ZoneOffset.UTC)

    private fun tipoDe(etapa: EtapaDaSaga): String = when (etapa) {
        EtapaDaSaga.RESERVA_DE_INSUMOS -> RESERVAR
        EtapaDaSaga.LIBERACAO_DE_INSUMOS -> LIBERAR
        EtapaDaSaga.CONSUMO_DE_INSUMOS -> CONSUMIR
        else -> throw IllegalArgumentException("Etapa $etapa não publica comando de estoque")
    }

    private fun traceAtual(): String? = Span.current().spanContext
        .takeIf { it.isValid }
        ?.let { "00-${it.traceId}-${it.spanId}-${it.traceFlags.asHex()}" }

    private fun traceIdDe(traceparent: String): String? = traceparent
        .split('-')
        .takeIf { it.size >= 3 }
        ?.get(1)

    private companion object {
        const val AGREGADO = "OrdemServico"
        const val VERSAO_DO_CONTRATO: Short = 1
        const val RESERVAR = "ReservarEstoque"
        const val LIBERAR = "LiberarReserva"
        const val CONSUMIR = "ConsumirReserva"
    }
}
