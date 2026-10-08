package com.clau.service_track.ordens.domain.saga

import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SagaTest {

    private val ordem = OrdemServicoId.gerar()
    private val oleo = InsumoId.gerar()
    private val filtro = InsumoId.gerar()
    private val pastilha = InsumoId.gerar()

    private fun reserva(insumos: List<InsumoId>): Saga = saga(TipoDeSaga.RESERVA, insumos)

    private fun consumo(insumos: List<InsumoId>): Saga = saga(TipoDeSaga.CONSUMO, insumos)

    private fun saga(tipo: TipoDeSaga, insumos: List<InsumoId>): Saga = Saga.abrir(
        ordemServicoId = ordem,
        tipo = tipo,
        insumos = insumos.associateWith { BigDecimal("2") },
        prazoDaEtapa = LocalDateTime.now().plusMinutes(2),
    )

    @Test
    fun `nome de toda etapa cabe no orcamento da chave de idempotencia`() {
        EtapaDaSaga.entries.forEach {
            assertTrue(
                it.name.length <= EtapaDaSaga.TETO_DO_NOME,
                "etapa ${it.name} tem ${it.name.length} caracteres e estoura o INBOX do catalogo",
            )
        }
    }

    @Test
    fun `saga abre em curso, com um passo pendente por insumo`() {
        val saga = reserva(listOf(oleo, filtro))

        assertEquals(SituacaoDaSaga.EM_CURSO, saga.situacao)
        assertEquals(EtapaDaSaga.RESERVA_DE_INSUMOS, saga.etapa)
        assertEquals(2, saga.passosPendentes().size)
    }

    @Test
    fun `saga sem insumo e recusada, porque nao tem o que orquestrar`() {
        assertFailsWith<DomainException> {
            Saga.abrir(ordem, TipoDeSaga.RESERVA, emptyMap(), LocalDateTime.now().plusMinutes(2))
        }
    }

    @Test
    fun `etapa so conclui com todos os passos confirmados`() {
        val saga = reserva(listOf(oleo, filtro))

        assertTrue(saga.confirmar(oleo))
        assertEquals(SituacaoDaSaga.EM_CURSO, saga.situacao)

        assertTrue(saga.confirmar(filtro))
        assertEquals(SituacaoDaSaga.CONCLUIDA, saga.situacao)
    }

    @Test
    fun `confirmacao repetida do mesmo passo nao muda nada`() {
        val saga = reserva(listOf(oleo, filtro))

        assertTrue(saga.confirmar(oleo))
        assertFalse(saga.confirmar(oleo))
        assertEquals(SituacaoDaSaga.EM_CURSO, saga.situacao)
    }

    @Test
    fun `confirmacao de insumo que nao e da saga e ignorada`() {
        val saga = reserva(listOf(oleo))

        assertFalse(saga.confirmar(pastilha))
        assertEquals(SituacaoDaSaga.EM_CURSO, saga.situacao)
    }

    @Test
    fun `recusa compensa so o que deu certo`() {
        val saga = reserva(listOf(oleo, filtro, pastilha))
        saga.confirmar(oleo)

        assertTrue(saga.recusar(filtro, "solicitado 2, disponivel 0"))

        assertEquals(SituacaoDaSaga.COMPENSANDO, saga.situacao)
        assertEquals(EtapaDaSaga.LIBERACAO_DE_INSUMOS, saga.etapa)
        assertEquals(setOf(oleo), saga.insumosAcompensar().keys)
        assertEquals(1, saga.passosPendentes().size)
        assertEquals(oleo, saga.passosPendentes().single().insumoId)
    }

    @Test
    fun `passo que nao respondeu antes da recusa e marcado como expirado, nao compensado`() {
        val saga = reserva(listOf(oleo, filtro, pastilha))
        saga.confirmar(oleo)
        saga.recusar(filtro, "sem saldo")

        val naEtapaDeReserva = saga.passosDaEtapa(EtapaDaSaga.RESERVA_DE_INSUMOS)
        assertEquals(SituacaoDoPasso.CONFIRMADO, naEtapaDeReserva.single { it.insumoId == oleo }.situacao)
        assertEquals(SituacaoDoPasso.RECUSADO, naEtapaDeReserva.single { it.insumoId == filtro }.situacao)
        assertEquals(SituacaoDoPasso.EXPIRADO, naEtapaDeReserva.single { it.insumoId == pastilha }.situacao)
    }

    @Test
    fun `recusa sem nada confirmado encerra compensada, sem passo de liberacao`() {
        val saga = reserva(listOf(oleo))

        saga.recusar(oleo, "sem saldo")

        assertEquals(SituacaoDaSaga.COMPENSADA, saga.situacao)
        assertTrue(saga.passosDaEtapa(EtapaDaSaga.LIBERACAO_DE_INSUMOS).isEmpty())
    }

    @Test
    fun `compensacao conclui quando toda liberacao confirma`() {
        val saga = reserva(listOf(oleo, filtro, pastilha))
        saga.confirmar(oleo)
        saga.confirmar(filtro)
        saga.recusar(pastilha, "sem saldo")

        assertTrue(saga.confirmar(oleo))
        assertEquals(SituacaoDaSaga.COMPENSANDO, saga.situacao)

        assertTrue(saga.confirmar(filtro))
        assertEquals(SituacaoDaSaga.COMPENSADA, saga.situacao)
        assertTrue(
            saga.passosDaEtapa(EtapaDaSaga.LIBERACAO_DE_INSUMOS).all { it.situacao == SituacaoDoPasso.COMPENSADO }
        )
    }

    @Test
    fun `segunda recusa durante a compensacao nao reabre compensacao`() {
        val saga = reserva(listOf(oleo, filtro, pastilha))
        saga.confirmar(oleo)
        saga.recusar(filtro, "sem saldo")
        val passosAntes = saga.listarPassos().size

        saga.recusar(oleo, "liberacao recusada")

        assertEquals(SituacaoDaSaga.COMPENSANDO, saga.situacao)
        assertEquals(passosAntes, saga.listarPassos().size)
    }

    @Test
    fun `expiracao de um passo compensa como recusa`() {
        val saga = reserva(listOf(oleo, filtro))
        saga.confirmar(oleo)

        assertTrue(saga.expirar(filtro, "reserva expirada no catalogo"))

        assertEquals(SituacaoDaSaga.COMPENSANDO, saga.situacao)
        assertEquals("reserva expirada no catalogo", saga.motivo)
    }

    @Test
    fun `prazo vencido reprova a etapa e compensa o confirmado`() {
        val saga = reserva(listOf(oleo, filtro))
        saga.confirmar(oleo)
        val depoisDoPrazo = LocalDateTime.now().plusMinutes(5)

        assertTrue(saga.expirouEm(depoisDoPrazo))
        assertTrue(saga.reprovarPorPrazo(depoisDoPrazo))

        assertEquals(SituacaoDaSaga.COMPENSANDO, saga.situacao)
        assertEquals(Saga.PRAZO_VENCIDO, saga.motivo)
        assertEquals(setOf(oleo), saga.insumosAcompensar().keys)
    }

    @Test
    fun `prazo vencido sem passo pendente nao reprova nada`() {
        val saga = reserva(listOf(oleo))
        saga.confirmar(oleo)

        assertFalse(saga.expirouEm(LocalDateTime.now().plusMinutes(5)))
        assertFalse(saga.reprovarPorPrazo(LocalDateTime.now().plusMinutes(5)))
        assertEquals(SituacaoDaSaga.CONCLUIDA, saga.situacao)
    }

    @Test
    fun `saga de consumo recusada vai para falha, porque consumo nao tem compensacao`() {
        val saga = consumo(listOf(oleo, filtro))
        saga.confirmar(oleo)

        assertTrue(saga.recusar(filtro, "reserva inexistente ou ja consumida"))

        assertEquals(SituacaoDaSaga.FALHA, saga.situacao)
        assertEquals(EtapaDaSaga.CONSUMO_DE_INSUMOS, saga.etapa)
        assertTrue(saga.passosDaEtapa(EtapaDaSaga.LIBERACAO_DE_INSUMOS).isEmpty())
    }

    @Test
    fun `saga de consumo conclui quando todo consumo confirma`() {
        val saga = consumo(listOf(oleo, filtro))

        saga.confirmar(oleo)
        saga.confirmar(filtro)

        assertEquals(SituacaoDaSaga.CONCLUIDA, saga.situacao)
    }

    @Test
    fun `saga encerrada ignora prazo e resposta atrasada`() {
        val saga = reserva(listOf(oleo))
        saga.confirmar(oleo)

        assertFalse(saga.expirouEm(LocalDateTime.now().plusHours(1)))
        assertFalse(saga.confirmar(oleo))
        assertFalse(saga.recusar(oleo, "atrasada"))
    }

    @Test
    fun `passo com quantidade nao positiva e recusado`() {
        assertFailsWith<DomainException> {
            Saga.abrir(
                ordem,
                TipoDeSaga.RESERVA,
                mapOf(oleo to BigDecimal.ZERO),
                LocalDateTime.now().plusMinutes(2),
            )
        }
    }
}
