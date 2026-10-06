package com.clau.service_track.ordens.domain.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.ServicoId
import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemOrdemServicoTest {

    private val valor100 = ValorMonetario(BigDecimal("100.00"))
    private val valor200 = ValorMonetario(BigDecimal("200.00"))
    private val mecanicoId = UsuarioId.gerar()

    private fun buildItem(
        valor: ValorMonetario = valor100,
        ordemServicoId: OrdemServicoId = OrdemServicoId.gerar(),
        servicoId: ServicoId = ServicoId.gerar(),
    ): ItemOrdemServico = ItemOrdemServico.criar(
        servicoId = servicoId,
        ordemServicoId = ordemServicoId,
        valor = valor,
    )

    @Test
    fun `deve criar item com estado inicial correto`() {
        val item = buildItem()

        assertNotNull(item.id)
        assertFalse(item.feito)
        assertNull(item.mecanicoResponsavelId)
        assertNull(item.dataRealizacao)
        assertNull(item.observacao)
        assertEquals(valor100, item.valor)
    }

    @Test
    fun `deve gerar id único a cada criação`() {
        val a = buildItem()
        val b = buildItem()
        assertTrue(a.id != b.id)
    }

    @Test
    fun `deve atualizar valor antes da conclusão`() {
        val item = buildItem()
        item.atualizarValor(valor200)
        assertEquals(valor200, item.valor)
    }

    @Test
    fun `deve lançar exceção ao atualizar valor de item já concluído`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)
        item.concluir("OK")

        val ex = assertThrows<DomainException> {
            item.atualizarValor(valor200)
        }
        assertTrue(ex.message!!.contains("concluído"))
    }

    @Test
    fun `deve vincular mecânico com sucesso`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)
        assertEquals(mecanicoId, item.mecanicoResponsavelId)
    }

    @Test
    fun `deve lançar exceção ao vincular mecânico em item concluído`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)
        item.concluir("OK")

        val ex = assertThrows<DomainException> {
            item.vincularMecanico(UsuarioId.gerar())
        }
        assertTrue(ex.message!!.contains("concluído"))
    }

    @Test
    fun `deve lançar exceção ao vincular segundo mecânico sem desvincular o primeiro`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)

        val ex = assertThrows<DomainException> {
            item.vincularMecanico(UsuarioId.gerar())
        }
        assertTrue(ex.message!!.contains("Desvincule"))
    }

    @Test
    fun `deve desvincular mecânico com sucesso`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)
        item.desvincularMecanico()
        assertNull(item.mecanicoResponsavelId)
    }

    @Test
    fun `deve lançar exceção ao desvincular mecânico de item concluído`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)
        item.concluir("OK")

        val ex = assertThrows<DomainException> {
            item.desvincularMecanico()
        }
        assertTrue(ex.message!!.contains("concluído"))
    }

    @Test
    fun `deve concluir item com sucesso`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)
        item.concluir("Troca realizada com sucesso")

        assertTrue(item.feito)
        assertNotNull(item.dataRealizacao)
        assertEquals("Troca realizada com sucesso", item.observacao)
    }

    @Test
    fun `deve lançar exceção ao concluir item já concluído`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)
        item.concluir("OK")

        val ex = assertThrows<DomainException> {
            item.concluir("segunda vez")
        }
        assertTrue(ex.message!!.contains("já foi concluído"))
    }

    @Test
    fun `deve lançar exceção ao concluir sem mecânico vinculado`() {
        val item = buildItem()

        val ex = assertThrows<DomainException> {
            item.concluir("OK")
        }
        assertTrue(ex.message!!.contains("mecânico"))
    }

    @Test
    fun `deve lançar exceção ao concluir com observação vazia`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)

        val ex = assertThrows<DomainException> {
            item.concluir("")
        }
        assertTrue(ex.message!!.contains("vazia"))
    }

    @Test
    fun `deve lançar exceção ao concluir com observação em branco`() {
        val item = buildItem()
        item.vincularMecanico(mecanicoId)

        val ex = assertThrows<DomainException> {
            item.concluir("   ")
        }
        assertTrue(ex.message!!.contains("vazia"))
    }

    @Test
    fun `deve reconstituir item a partir dos dados de persistência`() {
        val id = com.clau.service_track.ordens.domain.ordemservico.vo.ItemOrdemServicoId.gerar()
        val servicoId = ServicoId.gerar()
        val osId = OrdemServicoId.gerar()
        val agora = java.time.LocalDateTime.now()

        val item = ItemOrdemServico.reconstituir(
            id = id,
            servicoId = servicoId,
            ordemServicoId = osId,
            valor = valor100,
            feito = true,
            mecanicoResponsavelId = mecanicoId,
            dataRealizacao = agora,
            observacao = "Feito",
            dataCriacao = agora,
            dataAtualizacao = agora,
        )

        assertEquals(id, item.id)
        assertEquals(servicoId, item.servicoId)
        assertEquals(osId, item.ordemServicoId)
        assertEquals(valor100, item.valor)
        assertTrue(item.feito)
        assertEquals(mecanicoId, item.mecanicoResponsavelId)
        assertEquals("Feito", item.observacao)
    }
}
