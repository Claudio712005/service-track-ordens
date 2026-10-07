package com.clau.service_track.ordens.domain.orcamento

import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrcamentoTest {

    private fun buildOrcamento(
        custoMaoDeObra: BigDecimal = BigDecimal("200.00"),
        custoInsumos: BigDecimal = BigDecimal("50.00")
    ): Orcamento {
        return Orcamento.gerar(
            custoMaoDeObra = ValorMonetario(custoMaoDeObra),
            custoInsumos = ValorMonetario(custoInsumos)
        )
    }

    @Test
    fun `deve gerar orçamento válido`() {
        val orcamento = buildOrcamento()

        assertNotNull(orcamento)
        assertFalse(orcamento.estaAprovado())
    }

    @Test
    fun `deve calcular valor total corretamente`() {
        val orcamento = buildOrcamento(
            custoMaoDeObra = BigDecimal("200.00"),
            custoInsumos = BigDecimal("50.00")
        )
        assertEquals(BigDecimal("250.00"), orcamento.valorTotal.valor)
    }

    @Test
    fun `deve calcular valor total com insumos zerados`() {
        val orcamento = buildOrcamento(
            custoMaoDeObra = BigDecimal("150.00"),
            custoInsumos = BigDecimal("0.00")
        )
        assertEquals(BigDecimal("150.00"), orcamento.valorTotal.valor)
    }

    @Test
    fun `deve aprovar orçamento`() {
        val orcamento = buildOrcamento()
        orcamento.aprovar()
        assertTrue(orcamento.estaAprovado())
    }

    @Test
    fun `deve registrar observação ao aprovar`() {
        val orcamento = buildOrcamento()
        orcamento.aprovar()
        assertTrue(orcamento.obterObservacao().contains("aprovado"))
    }

    @Test
    fun `deve lançar exceção ao aprovar orçamento já aprovado`() {
        val orcamento = buildOrcamento()
        orcamento.aprovar()

        val exception = assertThrows<IllegalStateException> {
            orcamento.aprovar()
        }
        assertEquals("Orçamento já foi aprovado", exception.message)
    }

    @Test
    fun `deve reprovar orçamento com motivo válido`() {
        val orcamento = buildOrcamento()
        orcamento.reprovar("Preço acima do esperado")

        assertFalse(orcamento.estaAprovado())
        assertTrue(orcamento.obterObservacao().contains("Preço acima do esperado"))
    }

    @Test
    fun `deve lançar exceção ao reprovar orçamento sem motivo`() {
        val orcamento = buildOrcamento()
        val exception = assertThrows<DomainException> {
            orcamento.reprovar("")
        }
        assertEquals("Motivo para reprovação do orçamento deve ser informado", exception.message)
    }

    @Test
    fun `deve lançar exceção ao reprovar orçamento com motivo em branco`() {
        val orcamento = buildOrcamento()
        val exception = assertThrows<DomainException> {
            orcamento.reprovar("   ")
        }
        assertEquals("Motivo para reprovação do orçamento deve ser informado", exception.message)
    }

    @Test
    fun `deve lançar exceção ao reprovar orçamento já aprovado`() {
        val orcamento = buildOrcamento()
        orcamento.aprovar()

        val exception = assertThrows<IllegalStateException> {
            orcamento.reprovar("Motivo qualquer")
        }
        assertEquals("Orçamento já aprovado não pode ser reprovado", exception.message)
    }

    @Test
    fun `deve reconstituir orcamento a partir de dados de persistencia`() {
        val id = com.clau.service_track.ordens.domain.orcamento.vo.OrcamentoId.gerar()
        val agora = java.time.LocalDateTime.now()

        val orcamento = Orcamento.reconstituir(
            id = id,
            dataCriacao = agora,
            dataAtualizacao = agora,
            custoMaoDeObra = ValorMonetario(BigDecimal("300.00")),
            custoInsumos = ValorMonetario(BigDecimal("50.00")),
            aprovado = true,
            observacao = "Orçamento aprovado"
        )

        assertEquals(id, orcamento.id)
        assertEquals(BigDecimal("350.00"), orcamento.valorTotal.valor)
        assertTrue(orcamento.estaAprovado())
        assertEquals("Orçamento aprovado", orcamento.obterObservacao())
    }
}
