package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.application.port.out.repository.FiltroDeOrdens
import com.clau.service_track.ordens.application.port.out.repository.TransicaoDeStatus
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.referencia.ServicoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import com.clau.service_track.ordens.infrastructure.adapter.out.mapper.OrdemServicoMapper
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrdemServicoRepositoryPostgresAdapter::class, HistoricoStatusRepositoryPostgresAdapter::class, OrdemServicoMapper::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:st_ord_adapter;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS ORDENS",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "management.otlp.tracing.export.enabled=false",
        "management.otlp.metrics.export.enabled=false",
    ]
)
class OrdemServicoRepositoryPostgresAdapterTest {

    @Autowired
    private lateinit var ordens: OrdemServicoRepositoryPostgresAdapter

    @Autowired
    private lateinit var historico: HistoricoStatusRepositoryPostgresAdapter

    private fun nova(
        clienteId: UsuarioId = UsuarioId.gerar(),
        veiculoId: VeiculoId = VeiculoId.gerar(),
        mecanicoId: UsuarioId = UsuarioId.gerar(),
    ) = OrdemServico.abrir(
        motivo = "revisao completa",
        clienteId = clienteId,
        mecanicoId = mecanicoId,
        veiculoId = veiculoId,
        observacao = "entrada pela recepcao",
    )

    @Test
    fun `ordem recem aberta volta do banco igual ao que entrou`() {
        val ordem = nova()

        ordens.salvar(ordem)
        val lida = assertNotNull(ordens.porId(ordem.id))

        assertEquals(ordem.id, lida.id)
        assertEquals(ordem.motivo, lida.motivo)
        assertEquals(ordem.observacao, lida.observacao)
        assertEquals(ordem.clienteId, lida.clienteId)
        assertEquals(ordem.veiculoId, lida.veiculoId)
        assertEquals(ordem.obterMecanicoId(), lida.obterMecanicoId())
        assertEquals(StatusOrdemServicoEnum.RECEBIDA, lida.obterStatus())
        assertNull(lida.obterOrcamento())
        assertTrue(lida.listarInsumos().isEmpty())
        assertTrue(lida.listarServicos().isEmpty())
    }

    @Test
    fun `ordem inexistente volta nula, nao excecao`() {
        assertNull(ordens.porId(OrdemServicoId.gerar()))
    }

    @Test
    fun `orcamento, insumos e servicos sobrevivem ao ciclo de gravacao`() {
        val ordem = nova()
        ordem.iniciarDiagnostico()
        val insumo = InsumoId.gerar()
        ordem.adicionarInsumo(insumo, BigDecimal("4.5"))
        val servico = ServicoId.gerar()
        ordem.adicionarServico(servico, ValorMonetario(BigDecimal("180.00")))
        ordem.gerarOrcamento(ValorMonetario(BigDecimal("180.00")), ValorMonetario(BigDecimal("220.50")))

        ordens.salvar(ordem)
        val lida = assertNotNull(ordens.porId(ordem.id))

        assertEquals(StatusOrdemServicoEnum.AGUARDANDO_APROVACAO, lida.obterStatus())
        assertEquals(0, lida.obterOrcamento()!!.valorTotal.valor.compareTo(BigDecimal("400.50")))
        assertEquals(insumo, lida.listarInsumos().single().insumoId)
        assertEquals(0, lida.listarInsumos().single().quantidade.compareTo(BigDecimal("4.5")))
        assertEquals(servico, lida.listarServicos().single().servicoId)
    }

    @Test
    fun `regravar reconcilia em vez de duplicar linha`() {
        val ordem = nova()
        ordem.iniciarDiagnostico()
        val insumo = InsumoId.gerar()
        ordem.adicionarInsumo(insumo, BigDecimal("2"))
        ordens.salvar(ordem)

        val recarregada = assertNotNull(ordens.porId(ordem.id))
        recarregada.adicionarInsumo(insumo, BigDecimal("1"))
        ordens.salvar(recarregada)

        val lida = assertNotNull(ordens.porId(ordem.id))
        assertEquals(1, lida.listarInsumos().size)
        assertEquals(0, lida.listarInsumos().single().quantidade.compareTo(BigDecimal("3")))
    }

    @Test
    fun `item removido do agregado sai do banco`() {
        val ordem = nova()
        ordem.iniciarDiagnostico()
        val insumo = InsumoId.gerar()
        ordem.adicionarInsumo(insumo, BigDecimal("2"))
        ordem.adicionarServico(ServicoId.gerar(), ValorMonetario(BigDecimal("90")))
        ordens.salvar(ordem)

        val recarregada = assertNotNull(ordens.porId(ordem.id))
        recarregada.removerInsumo(insumo)
        ordens.salvar(recarregada)

        assertTrue(assertNotNull(ordens.porId(ordem.id)).listarInsumos().isEmpty())
        assertEquals(1, assertNotNull(ordens.porId(ordem.id)).listarServicos().size)
    }

    @Test
    fun `aprovacao do orcamento persiste sem mudar o estado da ordem`() {
        val ordem = nova()
        ordem.iniciarDiagnostico()
        ordem.gerarOrcamento(ValorMonetario(BigDecimal("100")), ValorMonetario.zero())
        ordens.salvar(ordem)

        val recarregada = assertNotNull(ordens.porId(ordem.id))
        recarregada.aprovarOrcamento()
        ordens.salvar(recarregada)

        val lida = assertNotNull(ordens.porId(ordem.id))
        assertTrue(lida.obterOrcamento()!!.estaAprovado())
        assertEquals(StatusOrdemServicoEnum.AGUARDANDO_APROVACAO, lida.obterStatus())
    }

    @Test
    fun `prazo de conclusao atravessa o fuso sem deslocar`() {
        val ordem = nova()
        val prazo = LocalDateTime.now().plusDays(3).withNano(0)
        ordem.definirPrazoConclusao(prazo)

        ordens.salvar(ordem)

        assertEquals(prazo, assertNotNull(ordens.porId(ordem.id)).obterPrazoConclusao())
    }

    @Test
    fun `filtro por cliente, veiculo, mecanico e status combina por E`() {
        val cliente = UsuarioId.gerar()
        val veiculo = VeiculoId.gerar()
        val mecanico = UsuarioId.gerar()

        ordens.salvar(nova(clienteId = cliente, veiculoId = veiculo, mecanicoId = mecanico))
        ordens.salvar(nova(clienteId = cliente))
        val emDiagnostico = nova(clienteId = cliente).also { it.iniciarDiagnostico() }
        ordens.salvar(emDiagnostico)

        assertEquals(3L, ordens.listar(FiltroDeOrdens(clienteId = cliente), 0, 10).total)
        assertEquals(1L, ordens.listar(FiltroDeOrdens(veiculoId = veiculo), 0, 10).total)
        assertEquals(1L, ordens.listar(FiltroDeOrdens(mecanicoId = mecanico), 0, 10).total)
        assertEquals(
            1L,
            ordens.listar(FiltroDeOrdens(status = StatusOrdemServicoEnum.EM_DIAGNOSTICO), 0, 10).total,
        )
        assertEquals(
            0L,
            ordens.listar(
                FiltroDeOrdens(veiculoId = veiculo, status = StatusOrdemServicoEnum.EM_DIAGNOSTICO),
                0,
                10,
            ).total,
        )
    }

    @Test
    fun `listagem sem filtro pagina e informa o total`() {
        repeat(3) { ordens.salvar(nova()) }

        val pagina = ordens.listar(FiltroDeOrdens(), 0, 2)

        assertEquals(2, pagina.conteudo.size)
        assertEquals(3L, pagina.total)
        assertEquals(2, pagina.totalDePaginas)
    }

    @Test
    fun `historico grava e devolve em ordem cronologica`() {
        val ordem = nova()
        ordens.salvar(ordem)
        val agora = OffsetDateTime.now(ZoneOffset.UTC)

        historico.registrar(
            TransicaoDeStatus(ordem.id, null, StatusOrdemServicoEnum.RECEBIDA, "abertura", "corr-1", agora)
        )
        historico.registrar(
            TransicaoDeStatus(
                ordem.id,
                StatusOrdemServicoEnum.RECEBIDA,
                StatusOrdemServicoEnum.EM_DIAGNOSTICO,
                null,
                "corr-2",
                agora.plusMinutes(5),
            )
        )

        val trilha = historico.porOrdem(ordem.id)

        assertEquals(2, trilha.size)
        assertNull(trilha.first().statusAnterior)
        assertEquals("abertura", trilha.first().motivo)
        assertEquals(StatusOrdemServicoEnum.EM_DIAGNOSTICO, trilha.last().statusNovo)
        assertNull(trilha.last().motivo)
        assertEquals("corr-2", trilha.last().correlationId)
    }

    @Test
    fun `linha sem transicao sobrevive ao banco e se distingue das transicoes`() {
        val ordem = nova()
        ordens.salvar(ordem)
        val agora = OffsetDateTime.now(ZoneOffset.UTC)

        historico.registrar(
            TransicaoDeStatus(ordem.id, null, StatusOrdemServicoEnum.RECEBIDA, "abertura", "corr-1", agora)
        )
        historico.registrar(
            TransicaoDeStatus(
                ordem.id,
                StatusOrdemServicoEnum.EM_EXECUCAO,
                StatusOrdemServicoEnum.EM_EXECUCAO,
                "ordem bloqueada na etapa CONSUMO_DE_INSUMOS, tentativa 1: sem saldo",
                "corr-2",
                agora.plusMinutes(5),
            )
        )

        val trilha = historico.porOrdem(ordem.id)

        assertEquals(2, trilha.size)
        assertTrue(trilha.first().transicionou)
        assertTrue(!trilha.last().transicionou, "anterior igual a novo e fato sem transicao")
        assertEquals(StatusOrdemServicoEnum.EM_EXECUCAO, trilha.last().statusAnterior)
        assertTrue(trilha.last().motivo!!.contains("bloqueada"))
    }

    @Test
    fun `historico de outra ordem nao aparece`() {
        val ordem = nova()
        ordens.salvar(ordem)
        historico.registrar(
            TransicaoDeStatus(
                ordem.id, null, StatusOrdemServicoEnum.RECEBIDA, null, null, OffsetDateTime.now(ZoneOffset.UTC),
            )
        )

        assertTrue(historico.porOrdem(OrdemServicoId.gerar()).isEmpty())
    }
}
