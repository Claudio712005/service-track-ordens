package com.clau.service_track.ordens.application.handler.ordemservico

import com.clau.service_track.ordens.CorrelacaoFixaAdapter
import com.clau.service_track.ordens.HistoricoStatusRepositoryMemoriaAdapter
import com.clau.service_track.ordens.OrdemServicoRepositoryMemoriaAdapter
import com.clau.service_track.ordens.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AbrirOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AdicionarInsumoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AdicionarServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AprovarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.CancelarOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConcluirItemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.DefinirPrazoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.EntregarOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.FinalizarOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.GerarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarDiagnosticoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarExecucaoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReatribuirMecanicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverInsumoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReprovarOrcamentoCommand
import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.ItemOrdemServicoId
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.referencia.ServicoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrdemServicoCommandHandlerTest {

    private val ordens = OrdemServicoRepositoryMemoriaAdapter()
    private val historico = HistoricoStatusRepositoryMemoriaAdapter()
    private val handler = OrdemServicoCommandHandler(ordens, historico, CorrelacaoFixaAdapter(CORRELACAO))

    @BeforeTest
    fun preparar() {
        ordens.reiniciar()
        historico.reiniciar()
    }

    private fun abrir(prazo: LocalDateTime? = null): OrdemServico = handler.executar(
        AbrirOrdemServicoCommand(
            motivo = "barulho na suspensao dianteira",
            clienteId = UsuarioId.gerar(),
            mecanicoId = UsuarioId.gerar(),
            veiculoId = VeiculoId.gerar(),
            observacao = "cliente relata ruido em lombada",
            prazoConclusao = prazo,
        )
    )

    private fun emDiagnostico(): OrdemServico {
        val ordem = abrir()
        return handler.executar(IniciarDiagnosticoCommand(ordem.id))
    }

    private fun aguardandoAprovacao(): OrdemServico {
        val ordem = emDiagnostico()
        handler.executar(AdicionarInsumoCommand(ordem.id, InsumoId.gerar(), BigDecimal("4")))
        return handler.executar(GerarOrcamentoCommand(ordem.id, ValorMonetario(BigDecimal("300")), ValorMonetario(BigDecimal("180"))))
    }

    private fun emExecucao(): OrdemServico {
        val ordem = aguardandoAprovacao()
        handler.executar(AprovarOrcamentoCommand(ordem.id))
        return handler.executar(IniciarExecucaoCommand(ordem.id))
    }

    @Test
    fun `abrir grava a ordem e registra a abertura no historico`() {
        val ordem = abrir()

        assertEquals(StatusOrdemServicoEnum.RECEBIDA, ordem.obterStatus())
        assertNotNull(ordens.porId(ordem.id))

        val trilha = historico.porOrdem(ordem.id)
        assertEquals(1, trilha.size)
        assertNull(trilha.first().statusAnterior)
        assertEquals(StatusOrdemServicoEnum.RECEBIDA, trilha.first().statusNovo)
        assertEquals(CORRELACAO, trilha.first().correlationId)
    }

    @Test
    fun `abrir com prazo define o prazo na mesma operacao`() {
        val prazo = LocalDateTime.now().plusDays(2)
        val ordem = abrir(prazo)

        assertEquals(prazo, ordem.obterPrazoConclusao())
    }

    @Test
    fun `comando em ordem inexistente responde recurso nao encontrado`() {
        val inexistente = OrdemServicoId.gerar()

        assertFailsWith<RecursoNaoEncontradoException> {
            handler.executar(IniciarDiagnosticoCommand(inexistente))
        }
    }

    @Test
    fun `transicao registra o estado anterior e o novo`() {
        val ordem = emDiagnostico()

        val trilha = historico.porOrdem(ordem.id)
        assertEquals(2, trilha.size)
        assertEquals(StatusOrdemServicoEnum.RECEBIDA, trilha.last().statusAnterior)
        assertEquals(StatusOrdemServicoEnum.EM_DIAGNOSTICO, trilha.last().statusNovo)
    }

    @Test
    fun `operacao que nao muda o estado nao suja o historico`() {
        val ordem = emDiagnostico()
        val antes = historico.porOrdem(ordem.id).size

        handler.executar(AdicionarInsumoCommand(ordem.id, InsumoId.gerar(), BigDecimal("2")))
        handler.executar(AdicionarServicoCommand(ordem.id, ServicoId.gerar(), ValorMonetario(BigDecimal("80"))))

        assertEquals(antes, historico.porOrdem(ordem.id).size)
    }

    @Test
    fun `insumo entra, soma e sai`() {
        val ordem = emDiagnostico()
        val insumo = InsumoId.gerar()

        handler.executar(AdicionarInsumoCommand(ordem.id, insumo, BigDecimal("2")))
        val comSoma = handler.executar(AdicionarInsumoCommand(ordem.id, insumo, BigDecimal("1.5")))

        assertEquals(1, comSoma.listarInsumos().size)
        assertEquals(0, comSoma.listarInsumos().first().quantidade.compareTo(BigDecimal("3.5")))

        val semInsumo = handler.executar(RemoverInsumoCommand(ordem.id, insumo))
        assertTrue(semInsumo.listarInsumos().isEmpty())
    }

    @Test
    fun `servico entra e sai`() {
        val ordem = emDiagnostico()
        val servico = ServicoId.gerar()

        val com = handler.executar(AdicionarServicoCommand(ordem.id, servico, ValorMonetario(BigDecimal("120"))))
        assertEquals(1, com.listarServicos().size)

        val sem = handler.executar(RemoverServicoCommand(ordem.id, servico))
        assertTrue(sem.listarServicos().isEmpty())
    }

    @Test
    fun `gerar orcamento leva para aguardando aprovacao e grava o total no motivo`() {
        val ordem = aguardandoAprovacao()

        assertEquals(StatusOrdemServicoEnum.AGUARDANDO_APROVACAO, ordem.obterStatus())
        assertEquals(0, ordem.obterOrcamento()!!.valorTotal.valor.compareTo(BigDecimal("480")))

        val transicao = historico.porOrdem(ordem.id).last()
        assertEquals(StatusOrdemServicoEnum.AGUARDANDO_APROVACAO, transicao.statusNovo)
        assertTrue(transicao.motivo!!.contains("480"))
    }

    @Test
    fun `aprovar orcamento nao transiciona e nao registra historico`() {
        val ordem = aguardandoAprovacao()
        val antes = historico.porOrdem(ordem.id).size

        val aprovada = handler.executar(AprovarOrcamentoCommand(ordem.id))

        assertEquals(StatusOrdemServicoEnum.AGUARDANDO_APROVACAO, aprovada.obterStatus())
        assertTrue(aprovada.obterOrcamento()!!.estaAprovado())
        assertEquals(antes, historico.porOrdem(ordem.id).size)
    }

    @Test
    fun `iniciar execucao e a confirmacao da saga de reserva`() {
        val ordem = emExecucao()

        assertEquals(StatusOrdemServicoEnum.EM_EXECUCAO, ordem.obterStatus())
        assertEquals("reserva de insumos confirmada", historico.porOrdem(ordem.id).last().motivo)
    }

    @Test
    fun `iniciar execucao sem aprovacao e recusado`() {
        val ordem = aguardandoAprovacao()

        assertFailsWith<IllegalStateException> { handler.executar(IniciarExecucaoCommand(ordem.id)) }
    }

    @Test
    fun `reprovar orcamento cancela a ordem com o motivo no historico`() {
        val ordem = aguardandoAprovacao()

        val cancelada = handler.executar(ReprovarOrcamentoCommand(ordem.id, "valor acima do combinado"))

        assertEquals(StatusOrdemServicoEnum.CANCELADA, cancelada.obterStatus())
        assertTrue(historico.porOrdem(ordem.id).last().motivo!!.contains("valor acima do combinado"))
    }

    @Test
    fun `concluir item de servico vincula o mecanico e marca como feito`() {
        val ordem = emDiagnostico()
        val servico = ServicoId.gerar()
        val comServico = handler.executar(AdicionarServicoCommand(ordem.id, servico, ValorMonetario(BigDecimal("120"))))
        val item = comServico.listarServicos().first()

        handler.executar(GerarOrcamentoCommand(ordem.id, ValorMonetario(BigDecimal("120")), ValorMonetario.zero()))
        handler.executar(AprovarOrcamentoCommand(ordem.id))
        handler.executar(IniciarExecucaoCommand(ordem.id))

        val mecanico = UsuarioId.gerar()
        val atualizada = handler.executar(
            ConcluirItemServicoCommand(ordem.id, item.id, mecanico, "amortecedor trocado")
        )

        val concluido = atualizada.listarServicos().first()
        assertTrue(concluido.feito)
        assertEquals(mecanico, concluido.mecanicoResponsavelId)
    }

    @Test
    fun `concluir item inexistente responde regra de negocio`() {
        val ordem = emExecucao()

        assertFailsWith<DomainException> {
            handler.executar(
                ConcluirItemServicoCommand(ordem.id, ItemOrdemServicoId.gerar(), UsuarioId.gerar(), "feito")
            )
        }
    }

    @Test
    fun `finalizar e entregar fecham o ciclo`() {
        val ordem = emExecucao()

        val finalizada = handler.executar(FinalizarOrdemServicoCommand(ordem.id))
        assertEquals(StatusOrdemServicoEnum.FINALIZADA, finalizada.obterStatus())
        assertEquals("consumo de insumos confirmado", historico.porOrdem(ordem.id).last().motivo)

        val entregue = handler.executar(EntregarOrdemServicoCommand(ordem.id))
        assertEquals(StatusOrdemServicoEnum.ENTREGUE, entregue.obterStatus())
        assertNull(historico.porOrdem(ordem.id).last().motivo)
    }

    @Test
    fun `cancelar sem motivo nao grava motivo em branco no historico`() {
        val ordem = abrir()

        val cancelada = handler.executar(CancelarOrdemServicoCommand(ordem.id, null))

        assertEquals(StatusOrdemServicoEnum.CANCELADA, cancelada.obterStatus())
        assertNull(historico.porOrdem(ordem.id).last().motivo)
    }

    @Test
    fun `cancelar com motivo guarda o motivo`() {
        val ordem = abrir()

        handler.executar(CancelarOrdemServicoCommand(ordem.id, "cliente desistiu"))

        assertEquals("cliente desistiu", historico.porOrdem(ordem.id).last().motivo)
    }

    @Test
    fun `definir prazo e reatribuir mecanico nao transicionam`() {
        val ordem = abrir()
        val antes = historico.porOrdem(ordem.id).size

        val prazo = LocalDateTime.now().plusDays(3)
        val comPrazo = handler.executar(DefinirPrazoCommand(ordem.id, prazo))
        assertEquals(prazo, comPrazo.obterPrazoConclusao())

        val novoMecanico = UsuarioId.gerar()
        val reatribuida = handler.executar(ReatribuirMecanicoCommand(ordem.id, novoMecanico))
        assertEquals(novoMecanico, reatribuida.obterMecanicoId())

        assertEquals(antes, historico.porOrdem(ordem.id).size)
    }

    @Test
    fun `falha na escrita nao deixa transicao no historico`() {
        val ordem = abrir()
        val antes = historico.porOrdem(ordem.id).size
        ordens.falharNaProximaEscrita = IllegalStateException("banco fora")

        assertFailsWith<IllegalStateException> { handler.executar(IniciarDiagnosticoCommand(ordem.id)) }

        assertEquals(antes, historico.porOrdem(ordem.id).size)
    }

    private companion object {
        const val CORRELACAO = "atendimento-88213"
    }
}
