package com.clau.service_track.ordens.bdd

import com.clau.service_track.ordens.CorrelacaoFixaAdapter
import com.clau.service_track.ordens.HistoricoStatusRepositoryMemoriaAdapter
import com.clau.service_track.ordens.OrdemServicoRepositoryMemoriaAdapter
import com.clau.service_track.ordens.OutboxMemoriaAdapter
import com.clau.service_track.ordens.SagaRepositoryMemoriaAdapter
import com.clau.service_track.ordens.application.handler.ordemservico.FluxoDaOrdemServico
import com.clau.service_track.ordens.application.handler.ordemservico.OrdemServicoCommandHandler
import com.clau.service_track.ordens.application.handler.saga.OrquestradorDaSaga
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AbrirOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AdicionarInsumoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.GerarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarDiagnosticoCommand
import com.clau.service_track.ordens.application.port.out.mensageria.MensagemParaPublicar
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import com.clau.service_track.ordens.domain.saga.EtapaDaSaga
import com.clau.service_track.ordens.domain.saga.PassoDaSaga
import com.clau.service_track.ordens.domain.saga.Saga
import com.clau.service_track.ordens.domain.saga.SituacaoDaSaga
import com.clau.service_track.ordens.domain.saga.TipoDeSaga
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import io.cucumber.java.Before
import io.cucumber.java.pt.Dado
import io.cucumber.java.pt.Então
import io.cucumber.java.pt.Quando
import java.math.BigDecimal
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PassosDaSaga {

    private val ordens = OrdemServicoRepositoryMemoriaAdapter()
    private val historico = HistoricoStatusRepositoryMemoriaAdapter()
    private val sagas = SagaRepositoryMemoriaAdapter()
    private val outbox = OutboxMemoriaAdapter()

    private val escrita = OrdemServicoCommandHandler(ordens, historico, CorrelacaoFixaAdapter(CORRELACAO))

    private val orquestrador = OrquestradorDaSaga(
        sagas = sagas,
        ordens = ordens,
        outbox = outbox,
        comandos = { saga, passos -> passos.map { comando(saga, it) } },
        iniciarExecucao = escrita,
        finalizar = escrita,
        cancelar = escrita,
        historico = historico,
        correlacao = CorrelacaoFixaAdapter(CORRELACAO),
        prazoDaEtapa = Duration.ofMinutes(2),
    )

    private val fluxo = FluxoDaOrdemServico(
        ordens = ordens,
        aprovar = escrita,
        iniciarExecucao = escrita,
        finalizar = escrita,
        orquestrador = orquestrador,
    )

    private var ordem: OrdemServicoId? = null

    private val ordemId: OrdemServicoId
        get() = requireNotNull(ordem) { "nenhuma ordem de servico aberta no cenario" }
    private val insumos = mutableMapOf<String, InsumoId>()

    @Before
    fun reiniciar() {
        ordens.reiniciar()
        historico.reiniciar()
        sagas.reiniciar()
        outbox.reiniciar()
        insumos.clear()
        ordem = null
    }

    private fun comando(saga: Saga, passo: PassoDaSaga) = MensagemParaPublicar(
        idMensagem = "${saga.ordemServicoId.valor}:${passo.etapa.name}:${passo.insumoId.valor}:${saga.tentativa}",
        agregadoTipo = "OrdemServico",
        agregadoId = saga.ordemServicoId.valor,
        chaveDeParticao = saga.ordemServicoId.valor,
        tipoMensagem = tipoDe(passo.etapa),
        versaoMensagem = 1,
        topico = "servicetrack.estoque.comandos.v1",
        payload = "{}",
        traceparent = null,
    )

    private fun tipoDe(etapa: EtapaDaSaga) = when (etapa) {
        EtapaDaSaga.RESERVA_DE_INSUMOS -> "ReservarEstoque"
        EtapaDaSaga.LIBERACAO_DE_INSUMOS -> "LiberarReserva"
        EtapaDaSaga.CONSUMO_DE_INSUMOS -> "ConsumirReserva"
        else -> etapa.name
    }

    private fun insumo(nome: String): InsumoId = insumos.getOrPut(nome) { InsumoId.gerar() }

    private fun saga(tipo: String): Saga? = sagas.porOrdemETipo(ordemId, TipoDeSaga.valueOf(tipo))

    @Dado("uma ordem de serviço aberta em diagnóstico")
    fun ordemAbertaEmDiagnostico() {
        val aberta = escrita.executar(
            AbrirOrdemServicoCommand(
                motivo = "barulho na suspensao dianteira",
                clienteId = UsuarioId.gerar(),
                mecanicoId = UsuarioId.gerar(),
                veiculoId = VeiculoId.gerar(),
            )
        )
        ordem = aberta.id
        escrita.executar(IniciarDiagnosticoCommand(ordemId))
    }

    @Dado("que a ordem precisa de {int} unidades do insumo {string}")
    fun ordemPrecisaDe(quantidade: Int, nome: String) {
        escrita.executar(AdicionarInsumoCommand(ordemId, insumo(nome), BigDecimal(quantidade)))
    }

    @Dado("que a ordem precisa de {int} unidade do insumo {string}")
    fun ordemPrecisaDeUma(quantidade: Int, nome: String) = ordemPrecisaDe(quantidade, nome)

    @Quando("o orçamento de {bigdecimal} de mão de obra e {bigdecimal} de insumos é gerado")
    fun orcamentoGerado(maoDeObra: BigDecimal, insumosDoOrcamento: BigDecimal) {
        escrita.executar(
            GerarOrcamentoCommand(ordemId, ValorMonetario(maoDeObra), ValorMonetario(insumosDoOrcamento))
        )
    }

    @Quando("o orçamento é aprovado")
    fun orcamentoAprovado() {
        fluxo.aprovarOrcamento(ordemId)
    }

    @Quando("a finalização é pedida")
    fun finalizacaoPedida() {
        fluxo.pedirFinalizacao(ordemId)
    }

    @Quando("o catálogo confirma a reserva do insumo {string}")
    fun catalogoConfirmaReserva(nome: String) {
        orquestrador.confirmarPasso(ordemId, insumo(nome))
    }

    @Quando("o catálogo confirma o consumo do insumo {string}")
    fun catalogoConfirmaConsumo(nome: String) = catalogoConfirmaReserva(nome)

    @Quando("o catálogo confirma a liberação do insumo {string}")
    fun catalogoConfirmaLiberacao(nome: String) = catalogoConfirmaReserva(nome)

    @Quando("o catálogo recusa a reserva do insumo {string} por {string}")
    fun catalogoRecusaReserva(nome: String, motivo: String) {
        orquestrador.recusarPasso(ordemId, insumo(nome), motivo)
    }

    @Quando("o catálogo recusa o consumo do insumo {string} por {string}")
    fun catalogoRecusaConsumo(nome: String, motivo: String) = catalogoRecusaReserva(nome, motivo)

    @Quando("o prazo da etapa vence")
    fun prazoVence() {
        sagas.vencerPrazos()
    }

    @Quando("a varredura de prazo roda")
    fun varreduraRoda() {
        orquestrador.reprovarProximaVencida()
        sagas.restaurarRelogio()
    }

    @Então("a ordem está em {string}")
    fun ordemEstaEm(estado: String) {
        assertEquals(
            StatusOrdemServicoEnum.valueOf(estado),
            assertNotNull(ordens.porId(ordemId)).obterStatus(),
        )
    }

    @Então("a ordem continua em {string}")
    fun ordemContinuaEm(estado: String) = ordemEstaEm(estado)

    @Então("a saga de {string} está {string} com {int} passos pendentes")
    fun sagaEsta(tipo: String, situacao: String, pendentes: Int) {
        val saga = assertNotNull(saga(tipo), "nao ha saga de $tipo nesta ordem")
        assertEquals(SituacaoDaSaga.valueOf(situacao), saga.situacao)
        assertEquals(pendentes, saga.passosPendentes().size)
    }

    @Então("a saga de {string} está na tentativa {int}")
    fun sagaNaTentativa(tipo: String, tentativa: Int) {
        assertEquals(tentativa, assertNotNull(saga(tipo)).tentativa)
    }

    @Então("não existe saga de {string}")
    fun naoExisteSaga(tipo: String) {
        assertNull(saga(tipo))
    }

    @Então("um comando {string} foi publicado para cada insumo")
    fun comandoPublicadoParaCadaInsumo(tipo: String) {
        val publicados = outbox.enfileiradas.filter { it.tipoMensagem == tipo }
        assertEquals(insumos.size, publicados.size)
        insumos.values.forEach { insumoId ->
            assertTrue(
                publicados.any { it.idMensagem.contains(insumoId.valor) },
                "nenhum $tipo publicado para ${insumoId.valor}",
            )
        }
    }

    @Então("um comando {string} foi publicado para o insumo {string}")
    fun comandoPublicadoPara(tipo: String, nome: String) {
        assertTrue(
            outbox.enfileiradas.any {
                it.tipoMensagem == tipo && it.idMensagem.contains(insumo(nome).valor)
            },
            "nenhum $tipo publicado para $nome",
        )
    }

    @Então("nenhum comando {string} foi publicado para o insumo {string}")
    fun nenhumComandoPublicadoPara(tipo: String, nome: String) {
        assertTrue(
            outbox.enfileiradas.none {
                it.tipoMensagem == tipo && it.idMensagem.contains(insumo(nome).valor)
            },
            "$tipo foi publicado para $nome, e nao devia",
        )
    }

    @Então("o comando republicado para o insumo {string} tem chave de tentativa {int}")
    fun chaveDeTentativa(nome: String, tentativa: Int) {
        assertTrue(
            outbox.enfileiradas.any {
                it.idMensagem.contains(insumo(nome).valor) && it.idMensagem.endsWith(":$tentativa")
            },
            "sem chave de tentativa $tentativa o destino engoliria a retentativa",
        )
    }

    @Então("o histórico da ordem registra {string}")
    fun historicoRegistra(trecho: String) {
        assertTrue(
            historico.porOrdem(ordemId).any { it.motivo?.contains(trecho) == true },
            "o historico nao registra '$trecho': ${historico.porOrdem(ordemId).map { it.motivo }}",
        )
    }

    @Então("o histórico da ordem tem {int} fato sem transição")
    fun historicoTemFatoSemTransicao(quantidade: Int) {
        assertEquals(quantidade, historico.porOrdem(ordemId).count { !it.transicionou })
    }

    private companion object {
        const val CORRELACAO = "atendimento-88213"
    }
}
