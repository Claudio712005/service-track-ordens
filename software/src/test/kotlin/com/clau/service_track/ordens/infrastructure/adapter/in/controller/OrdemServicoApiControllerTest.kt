package com.clau.service_track.ordens.infrastructure.adapter.`in`.controller

import com.clau.service_track.ordens.CorrelacaoFixaAdapter
import com.clau.service_track.ordens.HistoricoStatusRepositoryMemoriaAdapter
import com.clau.service_track.ordens.OrdemServicoRepositoryMemoriaAdapter
import com.clau.service_track.ordens.OutboxMemoriaAdapter
import com.clau.service_track.ordens.SagaRepositoryMemoriaAdapter
import com.clau.service_track.ordens.application.handler.ordemservico.FluxoDaOrdemServico
import com.clau.service_track.ordens.application.handler.ordemservico.OrdemServicoCommandHandler
import com.clau.service_track.ordens.application.handler.saga.OrquestradorDaSaga
import com.clau.service_track.ordens.application.handler.ordemservico.OrdemServicoQueryHandler
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AbrirOrdemServicoCommand
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import com.clau.service_track.ordens.infrastructure.adapter.`in`.mapper.OrdemServicoWebMapper
import com.clau.service_track.ordens.infrastructure.adapter.web.error.FabricaDeErro
import com.clau.service_track.ordens.infrastructure.adapter.web.error.GlobalExceptionHandler
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class OrdemServicoApiControllerTest {

    private val ordens = OrdemServicoRepositoryMemoriaAdapter()
    private val historico = HistoricoStatusRepositoryMemoriaAdapter()
    private val escrita = OrdemServicoCommandHandler(ordens, historico, CorrelacaoFixaAdapter("atendimento-1"))
    private val leitura = OrdemServicoQueryHandler(ordens, historico)
    private val mapper = OrdemServicoWebMapper()
    private val sagas = SagaRepositoryMemoriaAdapter()
    private val outbox = OutboxMemoriaAdapter()

    private val orquestrador = OrquestradorDaSaga(
        sagas = sagas,
        ordens = ordens,
        outbox = outbox,
        comandos = { _, _ -> emptyList() },
        iniciarExecucao = escrita,
        finalizar = escrita,
        cancelar = escrita,
        prazoDaEtapa = java.time.Duration.ofMinutes(2),
    )

    private val fluxo = FluxoDaOrdemServico(
        ordens = ordens,
        aprovar = escrita,
        iniciarExecucao = escrita,
        finalizar = escrita,
        orquestrador = orquestrador,
    )

    private lateinit var mockMvc: MockMvc

    @BeforeTest
    fun preparar() {
        ordens.reiniciar()
        historico.reiniciar()
        sagas.reiniciar()
        outbox.reiniciar()

        val controller = OrdemServicoApiController(
            abrir = escrita,
            consultar = leitura,
            listarOrdens = leitura,
            consultarHistorico = leitura,
            iniciarDiagnosticoUseCase = escrita,
            adicionarInsumoUseCase = escrita,
            removerInsumoUseCase = escrita,
            adicionarServicoUseCase = escrita,
            removerServicoUseCase = escrita,
            gerarOrcamentoUseCase = escrita,
            fluxo = fluxo,
            reprovarOrcamentoUseCase = escrita,
            iniciarExecucaoUseCase = escrita,
            concluirItemServicoUseCase = escrita,
            entregarUseCase = escrita,
            cancelarUseCase = escrita,
            definirPrazoUseCase = escrita,
            reatribuirMecanicoUseCase = escrita,
            mapper = mapper,
        )

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler(FabricaDeErro()))
            .build()
    }

    private fun ordemAberta(): OrdemServico = escrita.executar(
        AbrirOrdemServicoCommand(
            motivo = "troca de oleo e filtro",
            clienteId = UsuarioId.gerar(),
            mecanicoId = UsuarioId.gerar(),
            veiculoId = VeiculoId.gerar(),
        )
    )

    @Test
    fun `abertura responde 201 com o estado inicial`() {
        val corpo = """
            {
              "motivo": "troca de oleo e filtro",
              "clienteId": "${UUID.randomUUID()}",
              "mecanicoId": "${UUID.randomUUID()}",
              "veiculoId": "${UUID.randomUUID()}",
              "observacao": "cliente aguarda na loja"
            }
        """.trimIndent()

        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("RECEBIDA"))
            .andExpect(jsonPath("$.itensInsumo").isEmpty)
            .andExpect(jsonPath("$.orcamento").doesNotExist())
    }

    @Test
    fun `identificador fora do formato UUID responde 400 apontando o campo`() {
        val corpo = """
            {
              "motivo": "troca de oleo",
              "clienteId": "nao-e-uuid",
              "mecanicoId": "${UUID.randomUUID()}",
              "veiculoId": "${UUID.randomUUID()}"
            }
        """.trimIndent()

        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
            .andExpect(jsonPath("$.violacoes[0].campo").value("clienteId"))
    }

    @Test
    fun `motivo em branco responde 400`() {
        val corpo = """
            {
              "motivo": "   ",
              "clienteId": "${UUID.randomUUID()}",
              "mecanicoId": "${UUID.randomUUID()}",
              "veiculoId": "${UUID.randomUUID()}"
            }
        """.trimIndent()

        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.violacoes[0].campo").value("motivo"))
    }

    @Test
    fun `corpo que nao e json responde 400 com codigo de corpo ilegivel`() {
        mockMvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CORPO_ILEGIVEL"))
    }

    @Test
    fun `ordem inexistente responde 404`() {
        mockMvc.perform(get("/ordens/${UUID.randomUUID()}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"))
    }

    @Test
    fun `transicao invalida para o estado atual responde 409`() {
        val ordem = ordemAberta()

        mockMvc.perform(post("/ordens/${ordem.id.valor}/finalizacao"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CONFLITO_DE_ESTADO"))
    }

    @Test
    fun `status inexistente no filtro responde 400`() {
        mockMvc.perform(get("/ordens").param("status", "EM_LAVAGEM"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
    }

    @Test
    fun `listagem devolve a pagina com total`() {
        ordemAberta()
        ordemAberta()

        mockMvc.perform(get("/ordens").param("tamanho", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.conteudo.length()").value(1))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.totalDePaginas").value(2))
    }

    @Test
    fun `ordem sem insumo atravessa os endpoints sem saga`() {
        val ordem = ordemAberta()
        val id = ordem.id.valor
        val servico = UUID.randomUUID()

        mockMvc.perform(post("/ordens/$id/diagnostico"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"))

        mockMvc.perform(
            post("/ordens/$id/servicos").contentType(MediaType.APPLICATION_JSON)
                .content("""{"servicoId":"$servico","valor":180.00}""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/ordens/$id/orcamento").contentType(MediaType.APPLICATION_JSON)
                .content("""{"custoMaoDeObra":180.00,"custoInsumos":220.50}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
            .andExpect(jsonPath("$.orcamento.valorTotal").value(400.50))

        mockMvc.perform(post("/ordens/$id/orcamento/aprovacao"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
            .andExpect(jsonPath("$.orcamento.aprovado").value(true))

        mockMvc.perform(post("/ordens/$id/finalizacao"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("FINALIZADA"))

        mockMvc.perform(post("/ordens/$id/entrega"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ENTREGUE"))

        mockMvc.perform(get("/ordens/$id/historico"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(6))
            .andExpect(jsonPath("$[0].statusAnterior").doesNotExist())
            .andExpect(jsonPath("$[0].correlationId").value("atendimento-1"))
    }

    @Test
    fun `com insumo a aprovacao abre a saga e a ordem espera a confirmacao`() {
        val ordem = ordemAberta()
        val id = ordem.id.valor
        val insumo = UUID.randomUUID()

        mockMvc.perform(post("/ordens/$id/diagnostico")).andExpect(status().isOk)
        mockMvc.perform(
            post("/ordens/$id/insumos").contentType(MediaType.APPLICATION_JSON)
                .content("""{"insumoId":"$insumo","quantidade":4.5}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.itensInsumo[0].quantidade").value(4.5))

        mockMvc.perform(
            post("/ordens/$id/orcamento").contentType(MediaType.APPLICATION_JSON)
                .content("""{"custoMaoDeObra":180.00,"custoInsumos":220.50}""")
        ).andExpect(status().isOk)

        mockMvc.perform(post("/ordens/$id/orcamento/aprovacao"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
            .andExpect(jsonPath("$.orcamento.aprovado").value(true))

        val saga = sagas.porOrdemETipo(
            ordem.id,
            com.clau.service_track.ordens.domain.saga.TipoDeSaga.RESERVA,
        )
        kotlin.test.assertNotNull(saga, "aprovar o orcamento tem de abrir a saga de reserva")
        kotlin.test.assertEquals(1, saga.passosPendentes().size)
    }

    @Test
    fun `remocao de insumo e de servico respondem o estado atualizado`() {
        val ordem = ordemAberta()
        val id = ordem.id.valor
        val insumo = UUID.randomUUID()
        val servico = UUID.randomUUID()

        mockMvc.perform(post("/ordens/$id/diagnostico")).andExpect(status().isOk)
        mockMvc.perform(
            post("/ordens/$id/insumos").contentType(MediaType.APPLICATION_JSON)
                .content("""{"insumoId":"$insumo","quantidade":2}""")
        ).andExpect(status().isOk)
        mockMvc.perform(
            post("/ordens/$id/servicos").contentType(MediaType.APPLICATION_JSON)
                .content("""{"servicoId":"$servico","valor":90.00}""")
        ).andExpect(status().isOk)

        mockMvc.perform(delete("/ordens/$id/insumos/$insumo"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.itensInsumo").isEmpty)

        mockMvc.perform(delete("/ordens/$id/servicos/$servico"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.itensServico").isEmpty)
    }

    @Test
    fun `reprovacao de orcamento cancela e exige motivo`() {
        val ordem = ordemAberta()
        val id = ordem.id.valor

        mockMvc.perform(post("/ordens/$id/diagnostico")).andExpect(status().isOk)
        mockMvc.perform(
            post("/ordens/$id/orcamento").contentType(MediaType.APPLICATION_JSON)
                .content("""{"custoMaoDeObra":100.00,"custoInsumos":0.00}""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/ordens/$id/orcamento/reprovacao").contentType(MediaType.APPLICATION_JSON)
                .content("""{"motivo":"  "}""")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/ordens/$id/orcamento/reprovacao").contentType(MediaType.APPLICATION_JSON)
                .content("""{"motivo":"cliente achou caro"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELADA"))
    }

    @Test
    fun `cancelamento aceita corpo ausente`() {
        val ordem = ordemAberta()

        mockMvc.perform(post("/ordens/${ordem.id.valor}/cancelamento"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELADA"))
    }

    @Test
    fun `prazo no passado responde 400 e prazo futuro e aceito`() {
        val ordem = ordemAberta()
        val id = ordem.id.valor

        mockMvc.perform(
            put("/ordens/$id/prazo").contentType(MediaType.APPLICATION_JSON)
                .content("""{"prazoConclusao":"2020-01-01T10:00:00"}""")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            put("/ordens/$id/prazo").contentType(MediaType.APPLICATION_JSON)
                .content("""{"prazoConclusao":"2030-01-01T10:00:00"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.prazoConclusao").value("2030-01-01T10:00:00"))
    }

    @Test
    fun `reatribuir mecanico troca o responsavel`() {
        val ordem = ordemAberta()
        val novo = UUID.randomUUID()

        mockMvc.perform(
            put("/ordens/${ordem.id.valor}/mecanico").contentType(MediaType.APPLICATION_JSON)
                .content("""{"mecanicoId":"$novo"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mecanicoId").value(novo.toString()))
    }

    @Test
    fun `conclusao de item de servico responde o item feito`() {
        val ordem = ordemAberta()
        val id = ordem.id.valor
        val servico = UUID.randomUUID()
        val mecanico = UUID.randomUUID()

        mockMvc.perform(post("/ordens/$id/diagnostico")).andExpect(status().isOk)
        mockMvc.perform(
            post("/ordens/$id/servicos").contentType(MediaType.APPLICATION_JSON)
                .content("""{"servicoId":"$servico","valor":90.00}""")
        ).andExpect(status().isOk)
        mockMvc.perform(
            post("/ordens/$id/orcamento").contentType(MediaType.APPLICATION_JSON)
                .content("""{"custoMaoDeObra":90.00,"custoInsumos":0.00}""")
        ).andExpect(status().isOk)
        mockMvc.perform(post("/ordens/$id/orcamento/aprovacao"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))

        val itemId = ordens.porId(ordem.id)!!.listarServicos().first().id.valor

        mockMvc.perform(
            post("/ordens/$id/itens-servico/$itemId/conclusao").contentType(MediaType.APPLICATION_JSON)
                .content("""{"mecanicoId":"$mecanico","observacao":"pastilhas trocadas"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.itensServico[0].feito").value(true))
            .andExpect(jsonPath("$.itensServico[0].mecanicoResponsavelId").value(mecanico.toString()))
    }
}
