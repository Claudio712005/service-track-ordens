package com.clau.service_track.ordens.infrastructure.adapter.web.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.servlet.resource.NoResourceFoundException

class RotaInexistenteTest {

    private val handler = GlobalExceptionHandler(FabricaDeErro())

    private fun responder(metodo: String, caminho: String) =
        handler.rotaNaoEncontrada(
            NoResourceFoundException(HttpMethod.valueOf(metodo), caminho, caminho),
            MockHttpServletRequest(metodo, caminho),
        )

    @Test
    fun `rota inexistente responde 404, nao 500`() {
        val resposta = responder("GET", "/rota-que-nao-existe")

        assertEquals(HttpStatus.NOT_FOUND, resposta.statusCode)
    }

    @Test
    fun `o codigo devolvido e o de recurso nao encontrado`() {
        val corpo = assertNotNull(responder("GET", "/rota-que-nao-existe").body)

        assertEquals(CodigoErro.RECURSO_NAO_ENCONTRADO, corpo.code)
        assertEquals(404, corpo.status)
    }

    @Test
    fun `a mensagem nomeia metodo e caminho pedidos`() {
        val corpo = assertNotNull(responder("POST", "/ordens/xpto").body)

        assertTrue(corpo.message.contains("POST"), corpo.message)
        assertTrue(corpo.message.contains("/ordens/xpto"), corpo.message)
    }

    @Test
    fun `o caminho do actuator tambem responde 404 em vez de erro interno`() {
        val resposta = responder("GET", "/actuator/health/readiness")

        assertEquals(HttpStatus.NOT_FOUND, resposta.statusCode)
        assertEquals(CodigoErro.RECURSO_NAO_ENCONTRADO, assertNotNull(resposta.body).code)
    }
}
