package com.clau.service_track.ordens.application.handler.saga

import com.clau.service_track.ordens.CorrelacaoFixaAdapter
import com.clau.service_track.ordens.HistoricoStatusRepositoryMemoriaAdapter
import com.clau.service_track.ordens.OrdemServicoRepositoryMemoriaAdapter
import com.clau.service_track.ordens.OutboxMemoriaAdapter
import com.clau.service_track.ordens.SagaRepositoryMemoriaAdapter
import com.clau.service_track.ordens.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.ordens.application.handler.ordemservico.OrdemServicoCommandHandler
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AbrirOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AdicionarInsumoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AprovarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.GerarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarDiagnosticoCommand
import com.clau.service_track.ordens.application.port.out.mensageria.FabricaDeComandoDeEstoquePort
import com.clau.service_track.ordens.application.port.out.mensageria.MensagemParaPublicar
import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import com.clau.service_track.ordens.domain.saga.PassoDaSaga
import com.clau.service_track.ordens.domain.saga.Saga
import com.clau.service_track.ordens.domain.saga.SituacaoDaSaga
import com.clau.service_track.ordens.domain.saga.SituacaoDoPasso
import com.clau.service_track.ordens.domain.saga.TipoDeSaga
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.time.Duration
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrquestradorDaSagaTest {

    private val ordens = OrdemServicoRepositoryMemoriaAdapter()
    private val historico = HistoricoStatusRepositoryMemoriaAdapter()
    private val sagas = SagaRepositoryMemoriaAdapter()
    private val outbox = OutboxMemoriaAdapter()
    private val escrita = OrdemServicoCommandHandler(ordens, historico, CorrelacaoFixaAdapter("atendimento-1"))

    private val comandos = FabricaDeComandoDeEstoquePort { saga, passos ->
        passos.map { passo -> mensagem(saga, passo) }
    }

    private val orquestrador = OrquestradorDaSaga(
        sagas = sagas,
        ordens = ordens,
        outbox = outbox,
        comandos = comandos,
        iniciarExecucao = escrita,
        finalizar = escrita,
        cancelar = escrita,
        historico = historico,
        correlacao = CorrelacaoFixaAdapter("atendimento-1"),
        prazoDaEtapa = Duration.ofMinutes(2),
    )

    private val oleo = InsumoId.gerar()
    private val filtro = InsumoId.gerar()

    @BeforeTest
    fun preparar() {
        ordens.reiniciar()
        historico.reiniciar()
        sagas.reiniciar()
        outbox.reiniciar()
    }

    private fun mensagem(saga: Saga, passo: PassoDaSaga) = MensagemParaPublicar(
        idMensagem = "${saga.ordemServicoId.valor}:${passo.etapa.name}:${passo.insumoId.valor}:${saga.tentativa}",
        agregadoTipo = "OrdemServico",
        agregadoId = saga.ordemServicoId.valor,
        chaveDeParticao = saga.ordemServicoId.valor,
        tipoMensagem = passo.etapa.name,
        versaoMensagem = 1,
        topico = "servicetrack.estoque.comandos.v1",
        payload = "{}",
        traceparent = null,
    )

    private fun ordemAguardandoAprovacao(insumos: List<InsumoId> = listOf(oleo, filtro)): OrdemServico {
        val ordem = escrita.executar(
            AbrirOrdemServicoCommand(
                motivo = "barulho na suspensao",
                clienteId = UsuarioId.gerar(),
                mecanicoId = UsuarioId.gerar(),
                veiculoId = VeiculoId.gerar(),
            )
        )
        escrita.executar(IniciarDiagnosticoCommand(ordem.id))
        insumos.forEach { escrita.executar(AdicionarInsumoCommand(ordem.id, it, BigDecimal("2"))) }
        escrita.executar(
            GerarOrcamentoCommand(ordem.id, ValorMonetario(BigDecimal("300")), ValorMonetario(BigDecimal("180")))
        )
        return escrita.executar(AprovarOrcamentoCommand(ordem.id))
    }

    @Test
    fun `abrir reserva enfileira um comando por insumo`() {
        val ordem = ordemAguardandoAprovacao()

        val saga = orquestrador.abrirReserva(ordem.id)

        assertEquals(SituacaoDaSaga.EM_CURSO, saga.situacao)
        assertEquals(2, outbox.enfileiradas.size)
        assertTrue(outbox.enfileiradas.all { it.chaveDeParticao == ordem.id.valor })
    }

    @Test
    fun `abrir reserva duas vezes nao duplica comando`() {
        val ordem = ordemAguardandoAprovacao()

        orquestrador.abrirReserva(ordem.id)
        orquestrador.abrirReserva(ordem.id)

        assertEquals(2, outbox.enfileiradas.size)
    }

    @Test
    fun `ordem sem insumo nao abre saga`() {
        val ordem = escrita.executar(
            AbrirOrdemServicoCommand(
                motivo = "revisao",
                clienteId = UsuarioId.gerar(),
                mecanicoId = UsuarioId.gerar(),
                veiculoId = VeiculoId.gerar(),
            )
        )

        assertFailsWith<DomainException> { orquestrador.abrirReserva(ordem.id) }
    }

    @Test
    fun `ordem inexistente nao abre saga`() {
        assertFailsWith<RecursoNaoEncontradoException> { orquestrador.abrirReserva(OrdemServicoId.gerar()) }
    }

    @Test
    fun `reserva confirmada em todos os insumos leva a OS para execucao`() {
        val ordem = ordemAguardandoAprovacao()
        orquestrador.abrirReserva(ordem.id)

        orquestrador.confirmarPasso(ordem.id, oleo)
        assertEquals(StatusOrdemServicoEnum.AGUARDANDO_APROVACAO, ordens.porId(ordem.id)!!.obterStatus())

        orquestrador.confirmarPasso(ordem.id, filtro)
        assertEquals(StatusOrdemServicoEnum.EM_EXECUCAO, ordens.porId(ordem.id)!!.obterStatus())
    }

    @Test
    fun `recusa compensa o que foi reservado e cancela a OS`() {
        val ordem = ordemAguardandoAprovacao()
        orquestrador.abrirReserva(ordem.id)
        outbox.reiniciar()

        orquestrador.confirmarPasso(ordem.id, oleo)
        orquestrador.recusarPasso(ordem.id, filtro, "solicitado 2 LITRO, disponivel 0 LITRO")

        val liberacoes = outbox.enfileiradas.filter { it.tipoMensagem == "LIBERACAO_DE_INSUMOS" }
        assertEquals(1, liberacoes.size)
        assertTrue(liberacoes.single().idMensagem.contains(oleo.valor))
        assertEquals(StatusOrdemServicoEnum.AGUARDANDO_APROVACAO, ordens.porId(ordem.id)!!.obterStatus())

        orquestrador.confirmarPasso(ordem.id, oleo)

        val cancelada = ordens.porId(ordem.id)!!
        assertEquals(StatusOrdemServicoEnum.CANCELADA, cancelada.obterStatus())
        assertTrue(
            historico.porOrdem(ordem.id).last().motivo!!.contains("disponivel 0"),
            "o motivo da recusa do catalogo tem de chegar ao historico da OS",
        )
    }

    @Test
    fun `recusa com nada reservado cancela sem publicar liberacao`() {
        val ordem = ordemAguardandoAprovacao(listOf(oleo))
        orquestrador.abrirReserva(ordem.id)
        outbox.reiniciar()

        orquestrador.recusarPasso(ordem.id, oleo, "sem saldo")

        assertTrue(outbox.enfileiradas.isEmpty())
        assertEquals(StatusOrdemServicoEnum.CANCELADA, ordens.porId(ordem.id)!!.obterStatus())
    }

    @Test
    fun `confirmacao repetida nao reprocessa`() {
        val ordem = ordemAguardandoAprovacao(listOf(oleo))
        orquestrador.abrirReserva(ordem.id)

        assertTrue(orquestrador.confirmarPasso(ordem.id, oleo))
        assertFalse(orquestrador.confirmarPasso(ordem.id, oleo))
        assertEquals(StatusOrdemServicoEnum.EM_EXECUCAO, ordens.porId(ordem.id)!!.obterStatus())
    }

    @Test
    fun `evento de ordem sem saga aberta e ignorado`() {
        assertFalse(orquestrador.confirmarPasso(OrdemServicoId.gerar(), oleo))
        assertFalse(orquestrador.expirarPasso(OrdemServicoId.gerar(), oleo, "expirada"))
    }

    @Test
    fun `reserva expirada que a saga nao pediu compensa o resto`() {
        val ordem = ordemAguardandoAprovacao()
        orquestrador.abrirReserva(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        outbox.reiniciar()

        assertTrue(orquestrador.expirarPasso(ordem.id, filtro, "reserva expirada no catalogo"))

        assertEquals(1, outbox.enfileiradas.size)
        assertEquals(SituacaoDaSaga.COMPENSANDO, sagas.porOrdemETipo(ordem.id, TipoDeSaga.RESERVA)!!.situacao)
    }

    @Test
    fun `prazo vencido reprova a etapa e compensa`() {
        val ordem = ordemAguardandoAprovacao()
        orquestrador.abrirReserva(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        outbox.reiniciar()
        sagas.vencerPrazos()

        assertTrue(orquestrador.reprovarProximaVencida())

        sagas.restaurarRelogio()
        assertFalse(
            orquestrador.reprovarProximaVencida(),
            "a compensacao recebeu prazo novo; a varredura nao pode reprovar a mesma saga de novo",
        )

        assertEquals(1, outbox.enfileiradas.size)
        assertEquals(Saga.PRAZO_VENCIDO, sagas.porOrdemETipo(ordem.id, TipoDeSaga.RESERVA)!!.motivo)
    }

    @Test
    fun `consumo confirmado finaliza a OS`() {
        val ordem = ordemAguardandoAprovacao()
        orquestrador.abrirReserva(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        orquestrador.confirmarPasso(ordem.id, filtro)

        orquestrador.abrirConsumo(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        orquestrador.confirmarPasso(ordem.id, filtro)

        assertEquals(StatusOrdemServicoEnum.FINALIZADA, ordens.porId(ordem.id)!!.obterStatus())
    }

    @Test
    fun `consumo recusado vira falha, nao cancela a OS e fica visivel no historico`() {
        val ordem = ordemAguardandoAprovacao()
        orquestrador.abrirReserva(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        orquestrador.confirmarPasso(ordem.id, filtro)
        orquestrador.abrirConsumo(ordem.id)
        outbox.reiniciar()

        orquestrador.recusarPasso(ordem.id, oleo, "reserva inexistente ou ja consumida")

        val saga = sagas.porOrdemETipo(ordem.id, TipoDeSaga.CONSUMO)!!
        assertEquals(SituacaoDaSaga.FALHA, saga.situacao)
        assertTrue(outbox.enfileiradas.isEmpty())
        assertEquals(StatusOrdemServicoEnum.EM_EXECUCAO, ordens.porId(ordem.id)!!.obterStatus())

        val ultima = historico.porOrdem(ordem.id).last()
        assertFalse(ultima.transicionou, "bloqueio nao muda o estado da OS")
        assertEquals(StatusOrdemServicoEnum.EM_EXECUCAO, ultima.statusNovo)
        assertTrue(ultima.motivo!!.contains("bloqueada"))
        assertTrue(ultima.motivo!!.contains("CONSUMO_DE_INSUMOS"))
        assertTrue(ultima.motivo!!.contains("reserva inexistente ou ja consumida"))
        assertEquals("atendimento-1", ultima.correlationId)
    }

    @Test
    fun `bloqueio nao suja a regua de estados, porque e filtravel`() {
        val ordem = ordemAguardandoAprovacao()
        orquestrador.abrirReserva(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        orquestrador.confirmarPasso(ordem.id, filtro)
        orquestrador.abrirConsumo(ordem.id)
        orquestrador.recusarPasso(ordem.id, oleo, "sem saldo")

        val trilha = historico.porOrdem(ordem.id)

        assertEquals(1, trilha.count { !it.transicionou })
        assertTrue(trilha.filter { it.transicionou }.map { it.statusNovo }.containsAll(
            listOf(
                StatusOrdemServicoEnum.RECEBIDA,
                StatusOrdemServicoEnum.EM_DIAGNOSTICO,
                StatusOrdemServicoEnum.AGUARDANDO_APROVACAO,
                StatusOrdemServicoEnum.EM_EXECUCAO,
            )
        ))
    }

    @Test
    fun `consumo recusado e retentado depois da reposicao, com chave de tentativa nova`() {
        val ordem = ordemAguardandoAprovacao()
        orquestrador.abrirReserva(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        orquestrador.confirmarPasso(ordem.id, filtro)
        orquestrador.abrirConsumo(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        orquestrador.recusarPasso(ordem.id, filtro, "reserva inexistente ou ja consumida")

        assertEquals(SituacaoDaSaga.FALHA, sagas.porOrdemETipo(ordem.id, TipoDeSaga.CONSUMO)!!.situacao)
        outbox.reiniciar()

        val reaberta = orquestrador.abrirConsumo(ordem.id)

        assertEquals(SituacaoDaSaga.EM_CURSO, reaberta.situacao)
        assertEquals(2, reaberta.tentativa)
        assertEquals(1, outbox.enfileiradas.size)
        assertTrue(
            outbox.enfileiradas.single().idMensagem.endsWith(":2"),
            "a retentativa precisa de chave nova, senao o INBOX do destino a engole",
        )
        assertTrue(outbox.enfileiradas.single().idMensagem.contains(filtro.valor))

        orquestrador.confirmarPasso(ordem.id, filtro)
        assertEquals(StatusOrdemServicoEnum.FINALIZADA, ordens.porId(ordem.id)!!.obterStatus())
    }

    @Test
    fun `reabrir saga concluida nao republica nada`() {
        val ordem = ordemAguardandoAprovacao(listOf(oleo))
        orquestrador.abrirReserva(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        outbox.reiniciar()

        val mesma = orquestrador.abrirReserva(ordem.id)

        assertEquals(SituacaoDaSaga.CONCLUIDA, mesma.situacao)
        assertEquals(1, mesma.tentativa)
        assertTrue(outbox.enfileiradas.isEmpty())
    }

    @Test
    fun `passo da compensacao fica marcado como compensado`() {
        val ordem = ordemAguardandoAprovacao()
        orquestrador.abrirReserva(ordem.id)
        orquestrador.confirmarPasso(ordem.id, oleo)
        orquestrador.recusarPasso(ordem.id, filtro, "sem saldo")
        orquestrador.confirmarPasso(ordem.id, oleo)

        val saga = sagas.porOrdemETipo(ordem.id, TipoDeSaga.RESERVA)!!
        assertEquals(SituacaoDaSaga.COMPENSADA, saga.situacao)
        assertEquals(
            SituacaoDoPasso.COMPENSADO,
            saga.passosDaEtapa(com.clau.service_track.ordens.domain.saga.EtapaDaSaga.LIBERACAO_DE_INSUMOS)
                .single().situacao,
        )
    }
}
