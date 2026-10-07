package com.clau.service_track.ordens.application.handler.ordemservico

import com.clau.service_track.ordens.CorrelacaoFixaAdapter
import com.clau.service_track.ordens.HistoricoStatusRepositoryMemoriaAdapter
import com.clau.service_track.ordens.OrdemServicoRepositoryMemoriaAdapter
import com.clau.service_track.ordens.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AbrirOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConsultarHistoricoQuery
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarDiagnosticoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ListarOrdensServicoQuery
import com.clau.service_track.ordens.application.port.out.repository.FiltroDeOrdens
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrdemServicoQueryHandlerTest {

    private val ordens = OrdemServicoRepositoryMemoriaAdapter()
    private val historico = HistoricoStatusRepositoryMemoriaAdapter()
    private val escrita = OrdemServicoCommandHandler(ordens, historico, CorrelacaoFixaAdapter(null))
    private val leitura = OrdemServicoQueryHandler(ordens, historico)

    private val cliente = UsuarioId.gerar()
    private val veiculo = VeiculoId.gerar()

    @BeforeTest
    fun preparar() {
        ordens.reiniciar()
        historico.reiniciar()
    }

    private fun abrir(
        clienteId: UsuarioId = UsuarioId.gerar(),
        veiculoId: VeiculoId = VeiculoId.gerar(),
    ): OrdemServico = escrita.executar(
        AbrirOrdemServicoCommand(
            motivo = "revisao de 10 mil",
            clienteId = clienteId,
            mecanicoId = UsuarioId.gerar(),
            veiculoId = veiculoId,
        )
    )

    @Test
    fun `consulta por id devolve a ordem`() {
        val ordem = abrir()

        assertEquals(ordem.id, leitura.executar(ordem.id).id)
    }

    @Test
    fun `consulta de ordem inexistente responde recurso nao encontrado`() {
        assertFailsWith<RecursoNaoEncontradoException> { leitura.executar(OrdemServicoId.gerar()) }
    }

    @Test
    fun `listagem sem filtro devolve todas`() {
        abrir()
        abrir()
        abrir()

        val pagina = leitura.executar(ListarOrdensServicoQuery())

        assertEquals(3, pagina.conteudo.size)
        assertEquals(3L, pagina.total)
        assertEquals(1, pagina.totalDePaginas)
    }

    @Test
    fun `filtros sao combinados por E`() {
        abrir(clienteId = cliente, veiculoId = veiculo)
        abrir(clienteId = cliente)
        abrir(veiculoId = veiculo)

        val porCliente = leitura.executar(ListarOrdensServicoQuery(FiltroDeOrdens(clienteId = cliente)))
        assertEquals(2, porCliente.conteudo.size)

        val porAmbos = leitura.executar(
            ListarOrdensServicoQuery(FiltroDeOrdens(clienteId = cliente, veiculoId = veiculo))
        )
        assertEquals(1, porAmbos.conteudo.size)
    }

    @Test
    fun `filtro por status enxerga a transicao`() {
        val ordem = abrir()
        abrir()
        escrita.executar(IniciarDiagnosticoCommand(ordem.id))

        val emDiagnostico = leitura.executar(
            ListarOrdensServicoQuery(FiltroDeOrdens(status = StatusOrdemServicoEnum.EM_DIAGNOSTICO))
        )

        assertEquals(1, emDiagnostico.conteudo.size)
        assertEquals(ordem.id, emDiagnostico.conteudo.first().id)
    }

    @Test
    fun `paginacao recorta e informa o total de paginas`() {
        repeat(5) { abrir() }

        val primeira = leitura.executar(ListarOrdensServicoQuery(pagina = 0, tamanho = 2))
        assertEquals(2, primeira.conteudo.size)
        assertEquals(5L, primeira.total)
        assertEquals(3, primeira.totalDePaginas)

        val ultima = leitura.executar(ListarOrdensServicoQuery(pagina = 2, tamanho = 2))
        assertEquals(1, ultima.conteudo.size)
    }

    @Test
    fun `historico vem em ordem cronologica`() {
        val ordem = abrir()
        escrita.executar(IniciarDiagnosticoCommand(ordem.id))

        val trilha = leitura.executar(ConsultarHistoricoQuery(ordem.id))

        assertEquals(2, trilha.size)
        assertEquals(StatusOrdemServicoEnum.RECEBIDA, trilha.first().statusNovo)
        assertEquals(StatusOrdemServicoEnum.EM_DIAGNOSTICO, trilha.last().statusNovo)
        assertTrue(trilha.first().ocorridoEm <= trilha.last().ocorridoEm)
    }

    @Test
    fun `historico de ordem inexistente responde recurso nao encontrado, nao lista vazia`() {
        assertFailsWith<RecursoNaoEncontradoException> {
            leitura.executar(ConsultarHistoricoQuery(OrdemServicoId.gerar()))
        }
    }
}
