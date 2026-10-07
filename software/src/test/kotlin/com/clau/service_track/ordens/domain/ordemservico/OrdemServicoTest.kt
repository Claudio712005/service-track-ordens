package com.clau.service_track.ordens.domain.ordemservico

import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.clau.service_track.ordens.domain.ordemservico.vo.ItemOrdemServicoId

class OrdemServicoTest {

    private fun buildOS(): OrdemServico {
        return OrdemServico.abrir(
            motivo = "Troca de óleo e filtros",
            clienteId = UsuarioId.gerar(),
            mecanicoId = UsuarioId.gerar(),
            veiculoId = VeiculoId.gerar()
        )
    }

    private val custoMaoDeObra = ValorMonetario(BigDecimal("200.00"))
    private val custoInsumos = ValorMonetario(BigDecimal("100.00"))

    @Test
    fun `deve abrir OS com status RECEBIDA`() {
        val os = buildOS()
        assertEquals(StatusOrdemServicoEnum.RECEBIDA, os.obterStatus())
    }

    @Test
    fun `deve abrir OS sem orçamento`() {
        val os = buildOS()
        assertNull(os.obterOrcamento())
    }

    @Test
    fun `deve abrir OS sem insumos`() {
        val os = buildOS()
        assertTrue(os.listarInsumos().isEmpty())
    }

    @Test
    fun `deve lançar exceção ao abrir OS com motivo vazio`() {
        val exception = assertThrows<IllegalArgumentException> {
            OrdemServico.abrir(
                motivo = "",
                clienteId = UsuarioId.gerar(),
                mecanicoId = UsuarioId.gerar(),
                veiculoId = VeiculoId.gerar()
            )
        }
        assertEquals("Motivo da OS não pode ser vazio", exception.message)
    }

    @Test
    fun `deve transitar RECEBIDA para EM_DIAGNOSTICO`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        assertEquals(StatusOrdemServicoEnum.EM_DIAGNOSTICO, os.obterStatus())
    }

    @Test
    fun `deve transitar para AGUARDANDO_APROVACAO ao gerar orçamento`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.gerarOrcamento(custoMaoDeObra, custoInsumos)
        assertEquals(StatusOrdemServicoEnum.AGUARDANDO_APROVACAO, os.obterStatus())
    }

    @Test
    fun `deve transitar para EM_EXECUCAO ao aprovar orçamento`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.gerarOrcamento(custoMaoDeObra, custoInsumos)
        os.aprovarOrcamento()
        assertEquals(StatusOrdemServicoEnum.EM_EXECUCAO, os.obterStatus())
    }

    @Test
    fun `deve transitar para FINALIZADA`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.gerarOrcamento(custoMaoDeObra, custoInsumos)
        os.aprovarOrcamento()
        os.finalizar()
        assertEquals(StatusOrdemServicoEnum.FINALIZADA, os.obterStatus())
    }

    @Test
    fun `deve transitar para ENTREGUE`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.gerarOrcamento(custoMaoDeObra, custoInsumos)
        os.aprovarOrcamento()
        os.finalizar()
        os.entregar()
        assertEquals(StatusOrdemServicoEnum.ENTREGUE, os.obterStatus())
    }

    @Test
    fun `deve cancelar OS em qualquer etapa válida`() {
        val os = buildOS()
        os.cancelar("Solicitação do cliente")
        assertEquals(StatusOrdemServicoEnum.CANCELADA, os.obterStatus())
    }

    @Test
    fun `deve transitar para CANCELADA ao reprovar orçamento`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.gerarOrcamento(custoMaoDeObra, custoInsumos)
        os.reprovarOrcamento("Valor muito alto")
        assertEquals(StatusOrdemServicoEnum.CANCELADA, os.obterStatus())
    }

    @Test
    fun `deve lançar exceção para transição de status inválida`() {
        val os = buildOS()
        assertThrows<IllegalStateException> {
            os.finalizar()
        }
    }

    @Test
    fun `deve adicionar insumo durante diagnóstico`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val insumoId = InsumoId.gerar()

        os.adicionarInsumo(insumoId)

        assertTrue(os.listarInsumos().contains(insumoId))
    }

    @Test
    fun `deve adicionar múltiplos insumos durante diagnóstico`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val id1 = InsumoId.gerar()
        val id2 = InsumoId.gerar()

        os.adicionarInsumo(id1)
        os.adicionarInsumo(id2)

        assertEquals(2, os.listarInsumos().size)
    }

    @Test
    fun `deve lançar exceção ao adicionar insumo fora do diagnóstico`() {
        val os = buildOS()
        val exception = assertThrows<IllegalStateException> {
            os.adicionarInsumo(InsumoId.gerar())
        }
        assertTrue(exception.message!!.contains("diagnóstico"))
    }

    @Test
    fun `deve remover insumo durante diagnóstico`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val insumoId = InsumoId.gerar()
        os.adicionarInsumo(insumoId)

        os.removerInsumo(insumoId)

        assertFalse(os.listarInsumos().contains(insumoId))
    }

    @Test
    fun `deve lançar exceção ao remover insumo inexistente`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        assertThrows<IllegalStateException> {
            os.removerInsumo(InsumoId.gerar())
        }
    }

    @Test
    fun `listarInsumos deve retornar cópia defensiva`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.adicionarInsumo(InsumoId.gerar())

        val snapshot = os.listarInsumos()
        os.adicionarInsumo(InsumoId.gerar())

        assertEquals(1, snapshot.size)
        assertEquals(2, os.listarInsumos().size)
    }

    @Test
    fun `deve gerar orçamento com valores corretos`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.gerarOrcamento(custoMaoDeObra, custoInsumos)

        val orcamento = os.obterOrcamento()
        assertNotNull(orcamento)
        assertEquals(BigDecimal("200.00"), orcamento.custoMaoDeObra.valor)
        assertEquals(BigDecimal("100.00"), orcamento.custoInsumos.valor)
        assertEquals(BigDecimal("300.00"), orcamento.valorTotal.valor)
    }

    @Test
    fun `deve lançar exceção ao gerar orçamento fora do diagnóstico`() {
        val os = buildOS()
        val exception = assertThrows<IllegalStateException> {
            os.gerarOrcamento(custoMaoDeObra, custoInsumos)
        }
        assertTrue(exception.message!!.contains("diagnóstico"))
    }

    @Test
    fun `deve marcar orçamento como aprovado após aprovação`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.gerarOrcamento(custoMaoDeObra, custoInsumos)
        os.aprovarOrcamento()

        assertTrue(os.obterOrcamento()!!.estaAprovado())
    }

    @Test
    fun `deve lançar exceção ao aprovar OS sem orçamento`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        assertThrows<DomainException> {
            os.aprovarOrcamento()
        }
    }

    @Test
    fun `deve definir prazo de conclusão válido`() {
        val os = buildOS()
        val prazo = LocalDateTime.now().plusDays(3)
        os.definirPrazoConclusao(prazo)
        assertNotNull(os)
    }

    @Test
    fun `deve lançar exceção ao definir prazo no passado`() {
        val os = buildOS()
        val exception = assertThrows<DomainException> {
            os.definirPrazoConclusao(LocalDateTime.now().minusDays(1))
        }
        assertTrue(exception.message!!.contains("passado"))
    }

    @Test
    fun `deve lançar exceção ao redefinir prazo já existente`() {
        val os = buildOS()
        os.definirPrazoConclusao(LocalDateTime.now().plusDays(3))
        assertThrows<IllegalStateException> {
            os.definirPrazoConclusao(LocalDateTime.now().plusDays(5))
        }
    }

    @Test
    fun `deve reassinar mecânico diferente`() {
        val os = buildOS()
        val novoMecanicoId = UsuarioId.gerar()
        os.reassinarMecanico(novoMecanicoId)
        assertEquals(novoMecanicoId, os.obterMecanicoId())
    }

    @Test
    fun `deve lançar exceção ao reassinar mesmo mecânico`() {
        val mecanicoId = UsuarioId.gerar()
        val os = OrdemServico.abrir(
            motivo = "Revisão geral",
            clienteId = UsuarioId.gerar(),
            mecanicoId = mecanicoId,
            veiculoId = VeiculoId.gerar()
        )
        val exception = assertThrows<IllegalStateException> {
            os.reassinarMecanico(mecanicoId)
        }
        assertEquals("O mecânico já está atribuído a esta OS", exception.message)
    }

    @Test
    fun `deve abrir OS sem itens de serviço`() {
        val os = buildOS()
        assertTrue(os.listarServicos().isEmpty())
    }

    @Test
    fun `deve adicionar serviço durante diagnóstico`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val servicoId = com.clau.service_track.ordens.domain.referencia.ServicoId.gerar()

        val item = os.adicionarServico(servicoId, ValorMonetario(BigDecimal("80.00")))

        assertEquals(1, os.listarServicos().size)
        assertEquals(servicoId, item.servicoId)
        assertEquals(BigDecimal("80.00"), item.valor.valor)
        assertFalse(item.feito)
    }

    @Test
    fun `deve adicionar múltiplos serviços distintos durante diagnóstico`() {
        val os = buildOS()
        os.iniciarDiagnostico()

        os.adicionarServico(com.clau.service_track.ordens.domain.referencia.ServicoId.gerar(), ValorMonetario(BigDecimal("80.00")))
        os.adicionarServico(com.clau.service_track.ordens.domain.referencia.ServicoId.gerar(), ValorMonetario(BigDecimal("120.00")))

        assertEquals(2, os.listarServicos().size)
    }

    @Test
    fun `deve lançar exceção ao adicionar serviço fora do diagnóstico`() {
        val os = buildOS()
        val exception = assertThrows<IllegalStateException> {
            os.adicionarServico(com.clau.service_track.ordens.domain.referencia.ServicoId.gerar(), ValorMonetario(BigDecimal("80.00")))
        }
        assertTrue(exception.message!!.contains("diagnóstico"))
    }

    @Test
    fun `deve lançar exceção ao adicionar mesmo serviço duas vezes na mesma OS`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val servicoId = com.clau.service_track.ordens.domain.referencia.ServicoId.gerar()
        os.adicionarServico(servicoId, ValorMonetario(BigDecimal("80.00")))

        val exception = assertThrows<IllegalStateException> {
            os.adicionarServico(servicoId, ValorMonetario(BigDecimal("80.00")))
        }
        assertTrue(exception.message!!.contains("já adicionado"))
    }

    @Test
    fun `deve remover serviço durante diagnóstico`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val servicoId = com.clau.service_track.ordens.domain.referencia.ServicoId.gerar()
        os.adicionarServico(servicoId, ValorMonetario(BigDecimal("80.00")))

        os.removerServico(servicoId)

        assertTrue(os.listarServicos().isEmpty())
    }

    @Test
    fun `deve lançar exceção ao remover serviço inexistente`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val exception = assertThrows<com.clau.service_track.ordens.domain.DomainException> {
            os.removerServico(com.clau.service_track.ordens.domain.referencia.ServicoId.gerar())
        }
        assertTrue(exception.message!!.contains("não encontrado"))
    }

    @Test
    fun `deve lançar exceção ao remover serviço fora do diagnóstico`() {
        val os = buildOS()
        val exception = assertThrows<IllegalStateException> {
            os.removerServico(com.clau.service_track.ordens.domain.referencia.ServicoId.gerar())
        }
        assertTrue(exception.message!!.contains("diagnóstico"))
    }

    @Test
    fun `listarServicos deve retornar cópia defensiva`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.adicionarServico(com.clau.service_track.ordens.domain.referencia.ServicoId.gerar(), ValorMonetario(BigDecimal("80.00")))

        val snapshot = os.listarServicos()
        os.adicionarServico(com.clau.service_track.ordens.domain.referencia.ServicoId.gerar(), ValorMonetario(BigDecimal("50.00")))

        assertEquals(1, snapshot.size)
        assertEquals(2, os.listarServicos().size)
    }

    @Test
    fun `item adicionado deve referenciar o id da OS corretamente`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val servicoId = com.clau.service_track.ordens.domain.referencia.ServicoId.gerar()

        val item = os.adicionarServico(servicoId, ValorMonetario(BigDecimal("80.00")))

        assertEquals(os.id, item.ordemServicoId)
    }

    @Test
    fun `deve cancelar OS sem motivo sem alterar observacao`() {
        val os = buildOS()
        os.cancelar()
        assertEquals(StatusOrdemServicoEnum.CANCELADA, os.obterStatus())
    }

    @Test
    fun `deve lançar exceção ao reprovar orçamento quando OS não possui orçamento`() {
        val os = buildOS()
        os.iniciarDiagnostico()

        val exception = assertThrows<com.clau.service_track.ordens.domain.DomainException> {
            os.reprovarOrcamento("Valor alto")
        }
        assertEquals("OS não possui orçamento gerado", exception.message)
    }

    @Test
    fun `deve lançar exceção ao remover serviço já concluído`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val servicoId = com.clau.service_track.ordens.domain.referencia.ServicoId.gerar()
        val item = os.adicionarServico(servicoId, ValorMonetario(BigDecimal("80.00")))
        item.vincularMecanico(UsuarioId.gerar())
        item.concluir("Serviço executado")

        val exception = assertThrows<IllegalStateException> {
            os.removerServico(servicoId)
        }
        assertTrue(exception.message!!.contains("já concluído"))
    }

    @Test
    fun `deve reconstituir OS a partir de dados de persistencia`() {
        val id = com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId.gerar()
        val agora = LocalDateTime.now()
        val status = com.clau.service_track.ordens.domain.ordemservico.vo.StatusOrdemServico.deEnum(StatusOrdemServicoEnum.RECEBIDA)

        val os = OrdemServico.reconstituir(
            id = id,
            motivo = "Revisão geral",
            observacao = "",
            clienteId = UsuarioId.gerar(),
            mecanicoId = UsuarioId.gerar(),
            veiculoId = VeiculoId.gerar(),
            dataCriacao = agora,
            dataAtualizacao = agora,
            status = status,
            prazoConclusao = null,
            orcamento = null,
            insumos = mutableListOf(),
            itensServico = mutableListOf()
        )

        assertEquals(id, os.id)
        assertEquals("Revisão geral", os.motivo)
        assertEquals(StatusOrdemServicoEnum.RECEBIDA, os.obterStatus())
    }

    @Test
    fun `deve concluir item de servico e vincular mecanico automaticamente`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val servicoId = com.clau.service_track.ordens.domain.referencia.ServicoId.gerar()
        val item = os.adicionarServico(servicoId, ValorMonetario(BigDecimal("80.00")))
        os.gerarOrcamento(custoMaoDeObra, custoInsumos)
        os.aprovarOrcamento()

        val mecanicoId = UsuarioId.gerar()
        os.concluirItemServico(item.id, mecanicoId, "Servico executado")

        assertTrue(item.feito)
        assertEquals(mecanicoId, item.mecanicoResponsavelId)
        assertEquals("Servico executado", item.observacao)
        assertNotNull(item.dataRealizacao)
    }

    @Test
    fun `deve lançar exceção ao concluir item fora de execução`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        val item = os.adicionarServico(com.clau.service_track.ordens.domain.referencia.ServicoId.gerar(), ValorMonetario(BigDecimal("80.00")))

        val exception = assertThrows<IllegalStateException> {
            os.concluirItemServico(item.id, UsuarioId.gerar(), "Servico executado")
        }
        assertTrue(exception.message!!.contains("execução"))
    }

    @Test
    fun `deve lançar exceção ao concluir item inexistente`() {
        val os = buildOS()
        os.iniciarDiagnostico()
        os.gerarOrcamento(custoMaoDeObra, custoInsumos)
        os.aprovarOrcamento()

        val exception = assertThrows<DomainException> {
            os.concluirItemServico(ItemOrdemServicoId.gerar(), UsuarioId.gerar(), "Servico executado")
        }
        assertTrue(exception.message!!.contains("não encontrado"))
    }
}
