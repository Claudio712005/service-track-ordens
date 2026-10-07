package com.clau.service_track.ordens.infrastructure.adapter.web.filter

import com.clau.service_track.ordens.infrastructure.adapter.out.observabilidade.CorrelacaoMdcAdapter
import jakarta.servlet.FilterChain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.HandlerMapping

class CorrelacaoFilterTest {

    private val filtro = CorrelacaoFilter()

    private fun executar(
        requisicao: MockHttpServletRequest,
        resposta: MockHttpServletResponse = MockHttpServletResponse(),
        durante: (MockHttpServletRequest) -> Unit = {},
    ): MockHttpServletResponse {
        val cadeia = FilterChain { _, _ -> durante(requisicao) }
        filtro.doFilter(requisicao, resposta, cadeia)
        return resposta
    }

    @Test
    fun `correlacao ausente e gerada e devolvida no cabecalho`() {
        val resposta = executar(MockHttpServletRequest("GET", "/ordens"))

        val correlacao = assertNotNull(resposta.getHeader(CorrelacaoFilter.CABECALHO_CORRELACAO))
        assertTrue(correlacao.isNotBlank())
        assertNotNull(resposta.getHeader(CorrelacaoFilter.CABECALHO_REQUISICAO))
    }

    @Test
    fun `correlacao recebida e reaproveitada`() {
        val requisicao = MockHttpServletRequest("GET", "/ordens")
        requisicao.addHeader(CorrelacaoFilter.CABECALHO_CORRELACAO, "atendimento-88213")

        val resposta = executar(requisicao)

        assertEquals("atendimento-88213", resposta.getHeader(CorrelacaoFilter.CABECALHO_CORRELACAO))
    }

    @Test
    fun `correlacao com caractere fora do permitido e saneada`() {
        val requisicao = MockHttpServletRequest("GET", "/ordens")
        requisicao.addHeader(CorrelacaoFilter.CABECALHO_CORRELACAO, "atendimento 88213\n<script>")

        val resposta = executar(requisicao)

        assertEquals("atendimento88213script", resposta.getHeader(CorrelacaoFilter.CABECALHO_CORRELACAO))
    }

    @Test
    fun `correlacao que sobra em branco apos sanear vira identificador novo`() {
        val requisicao = MockHttpServletRequest("GET", "/ordens")
        requisicao.addHeader(CorrelacaoFilter.CABECALHO_CORRELACAO, "   ")

        val resposta = executar(requisicao)

        assertNotEquals("   ", resposta.getHeader(CorrelacaoFilter.CABECALHO_CORRELACAO))
        assertTrue(resposta.getHeader(CorrelacaoFilter.CABECALHO_CORRELACAO)!!.isNotBlank())
    }

    @Test
    fun `correlacao mais longa que o teto e truncada`() {
        val requisicao = MockHttpServletRequest("GET", "/ordens")
        requisicao.addHeader(CorrelacaoFilter.CABECALHO_CORRELACAO, "a".repeat(200))

        val resposta = executar(requisicao)

        assertEquals(64, resposta.getHeader(CorrelacaoFilter.CABECALHO_CORRELACAO)!!.length)
    }

    @Test
    fun `MDC tem a correlacao durante a requisicao e esta limpo depois`() {
        var vistaDentro: String? = null

        executar(MockHttpServletRequest("GET", "/ordens")) {
            vistaDentro = MDC.get(CorrelacaoFilter.CHAVE_CORRELACAO)
        }

        assertNotNull(vistaDentro)
        assertNull(MDC.get(CorrelacaoFilter.CHAVE_CORRELACAO))
        assertNull(MDC.get(CorrelacaoFilter.CHAVE_REQUISICAO))
    }

    @Test
    fun `a porta de correlacao le o que o filtro escreveu`() {
        val porta = CorrelacaoMdcAdapter()
        var vistaPelaPorta: String? = null

        val requisicao = MockHttpServletRequest("POST", "/ordens")
        requisicao.addHeader(CorrelacaoFilter.CABECALHO_CORRELACAO, "atendimento-1")

        executar(requisicao) { vistaPelaPorta = porta.correlacaoAtual() }

        assertEquals("atendimento-1", vistaPelaPorta)
        assertNull(porta.correlacaoAtual())
    }

    @Test
    fun `rota usa o template do handler quando existe, e o caminho quando nao`() {
        val comTemplate = MockHttpServletRequest("GET", "/ordens/123")
        comTemplate.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/ordens/{id}")
        assertEquals("/ordens/{id}", CorrelacaoFilter.rotaDe(comTemplate))

        val semTemplate = MockHttpServletRequest("GET", "/ordens/123")
        assertEquals("/ordens/123", CorrelacaoFilter.rotaDe(semTemplate))
    }

    @Test
    fun `limpa o MDC mesmo quando a cadeia falha`() {
        val requisicao = MockHttpServletRequest("POST", "/ordens")
        val cadeia = FilterChain { _, _ -> throw IllegalStateException("falha na cadeia") }

        runCatching { filtro.doFilter(requisicao, MockHttpServletResponse(), cadeia) }

        assertNull(MDC.get(CorrelacaoFilter.CHAVE_CORRELACAO))
    }
}
