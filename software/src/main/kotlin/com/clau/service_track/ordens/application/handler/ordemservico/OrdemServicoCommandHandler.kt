package com.clau.service_track.ordens.application.handler.ordemservico

import com.clau.service_track.ordens.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AbrirOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AbrirOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AdicionarInsumoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AdicionarInsumoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AdicionarServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AdicionarServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AprovarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AprovarOrcamentoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.CancelarOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.CancelarOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConcluirItemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConcluirItemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.DefinirPrazoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.DefinirPrazoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.EntregarOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.EntregarOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.FinalizarOrdemServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.FinalizarOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.GerarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.GerarOrcamentoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarDiagnosticoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarDiagnosticoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarExecucaoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.IniciarExecucaoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReatribuirMecanicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReatribuirMecanicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverInsumoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverInsumoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReprovarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReprovarOrcamentoUseCase
import com.clau.service_track.ordens.application.port.out.CorrelacaoPort
import com.clau.service_track.ordens.application.port.out.repository.HistoricoStatusRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.OrdemServicoRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.TransicaoDeStatus
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrdemServicoCommandHandler(
    private val ordens: OrdemServicoRepositoryPort,
    private val historico: HistoricoStatusRepositoryPort,
    private val correlacao: CorrelacaoPort,
) : AbrirOrdemServicoUseCase,
    IniciarDiagnosticoUseCase,
    AdicionarInsumoUseCase,
    RemoverInsumoUseCase,
    AdicionarServicoUseCase,
    RemoverServicoUseCase,
    GerarOrcamentoUseCase,
    AprovarOrcamentoUseCase,
    ReprovarOrcamentoUseCase,
    IniciarExecucaoUseCase,
    ConcluirItemServicoUseCase,
    FinalizarOrdemServicoUseCase,
    EntregarOrdemServicoUseCase,
    CancelarOrdemServicoUseCase,
    DefinirPrazoUseCase,
    ReatribuirMecanicoUseCase {

    private val log = LoggerFactory.getLogger(OrdemServicoCommandHandler::class.java)

    @Transactional
    override fun executar(comando: AbrirOrdemServicoCommand): OrdemServico {
        val ordem = OrdemServico.abrir(
            motivo = comando.motivo,
            clienteId = comando.clienteId,
            mecanicoId = comando.mecanicoId,
            veiculoId = comando.veiculoId,
            observacao = comando.observacao,
        )
        comando.prazoConclusao?.let(ordem::definirPrazoConclusao)

        val salva = ordens.salvar(ordem)
        registrar(salva.id, null, salva.obterStatus(), "abertura da ordem de servico")
        log.info(
            "ordem aberta ordemServicoId={} clienteId={} veiculoId={} status={}",
            salva.id.valor, salva.clienteId.valor, salva.veiculoId.valor, salva.obterStatus(),
        )
        return salva
    }

    @Transactional
    override fun executar(comando: IniciarDiagnosticoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { it.iniciarDiagnostico(); null }

    @Transactional
    override fun executar(comando: AdicionarInsumoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { it.adicionarInsumo(comando.insumoId, comando.quantidade); null }

    @Transactional
    override fun executar(comando: RemoverInsumoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { it.removerInsumo(comando.insumoId); null }

    @Transactional
    override fun executar(comando: AdicionarServicoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { it.adicionarServico(comando.servicoId, comando.valor); null }

    @Transactional
    override fun executar(comando: RemoverServicoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { it.removerServico(comando.servicoId); null }

    @Transactional
    override fun executar(comando: GerarOrcamentoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { ordem ->
            ordem.gerarOrcamento(comando.custoMaoDeObra, comando.custoInsumos)
            "orcamento gerado, total ${ordem.obterOrcamento()?.valorTotal?.valor}"
        }

    @Transactional
    override fun executar(comando: AprovarOrcamentoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { it.aprovarOrcamento(); null }

    @Transactional
    override fun executar(comando: ReprovarOrcamentoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { ordem ->
            ordem.reprovarOrcamento(comando.motivo)
            "orcamento reprovado: ${comando.motivo}"
        }

    @Transactional
    override fun executar(comando: IniciarExecucaoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { ordem ->
            ordem.iniciarExecucao()
            "reserva de insumos confirmada"
        }

    @Transactional
    override fun executar(comando: ConcluirItemServicoCommand): OrdemServico =
        mutar(comando.ordemServicoId) {
            it.concluirItemServico(comando.itemId, comando.mecanicoId, comando.observacao)
            null
        }

    @Transactional
    override fun executar(comando: FinalizarOrdemServicoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { ordem ->
            ordem.finalizar()
            "consumo de insumos confirmado"
        }

    @Transactional
    override fun executar(comando: EntregarOrdemServicoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { it.entregar(); null }

    @Transactional
    override fun executar(comando: CancelarOrdemServicoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { ordem ->
            ordem.cancelar(comando.motivo.orEmpty())
            comando.motivo
        }

    @Transactional
    override fun executar(comando: DefinirPrazoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { it.definirPrazoConclusao(comando.prazoConclusao); null }

    @Transactional
    override fun executar(comando: ReatribuirMecanicoCommand): OrdemServico =
        mutar(comando.ordemServicoId) { it.reassinarMecanico(comando.mecanicoId); null }

    private fun mutar(id: OrdemServicoId, operacao: (OrdemServico) -> String?): OrdemServico {
        val ordem = ordens.porId(id)
            ?: throw RecursoNaoEncontradoException("Ordem de serviço ${id.valor} não encontrada")

        val anterior = ordem.obterStatus()
        val motivo = operacao(ordem)
        val salva = ordens.salvar(ordem)
        val novo = salva.obterStatus()

        if (novo != anterior) {
            registrar(salva.id, anterior, novo, motivo)
            log.info("status alterado ordemServicoId={} de={} para={}", salva.id.valor, anterior, novo)
        }

        return salva
    }

    private fun registrar(
        id: OrdemServicoId,
        anterior: StatusOrdemServicoEnum?,
        novo: StatusOrdemServicoEnum,
        motivo: String?,
    ) {
        historico.registrar(
            TransicaoDeStatus(
                ordemServicoId = id,
                statusAnterior = anterior,
                statusNovo = novo,
                motivo = motivo?.takeIf { it.isNotBlank() },
                correlationId = correlacao.correlacaoAtual(),
                ocorridoEm = OffsetDateTime.now(ZoneOffset.UTC),
            )
        )
    }
}
