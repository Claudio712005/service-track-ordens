package com.clau.service_track.ordens.infrastructure.adapter.`in`.controller

import com.clau.service_track.ordens.application.port.`in`.api.OrdemServicoApiPort
import com.clau.service_track.ordens.application.port.`in`.api.dto.AbrirOrdemRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.AdicionarInsumoRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.AdicionarServicoRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.CancelarOrdemRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.ConcluirItemServicoRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.DefinirPrazoRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.GerarOrcamentoRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.OrdemServicoResponse
import com.clau.service_track.ordens.application.port.`in`.api.dto.PaginaResponse
import com.clau.service_track.ordens.application.port.`in`.api.dto.ReatribuirMecanicoRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.ReprovarOrcamentoRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.TransicaoDeStatusResponse
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
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConsultarHistoricoQuery
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConsultarHistoricoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConsultarOrdemServicoUseCase
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
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ListarOrdensServicoQuery
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ListarOrdensServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReatribuirMecanicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReatribuirMecanicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverInsumoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverInsumoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverServicoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.RemoverServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReprovarOrcamentoCommand
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ReprovarOrcamentoUseCase
import com.clau.service_track.ordens.application.port.out.repository.FiltroDeOrdens
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.ItemOrdemServicoId
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.referencia.ServicoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import com.clau.service_track.ordens.infrastructure.adapter.`in`.mapper.OrdemServicoWebMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class OrdemServicoApiController(
    private val abrir: AbrirOrdemServicoUseCase,
    private val consultar: ConsultarOrdemServicoUseCase,
    private val listarOrdens: ListarOrdensServicoUseCase,
    private val consultarHistorico: ConsultarHistoricoUseCase,
    private val iniciarDiagnosticoUseCase: IniciarDiagnosticoUseCase,
    private val adicionarInsumoUseCase: AdicionarInsumoUseCase,
    private val removerInsumoUseCase: RemoverInsumoUseCase,
    private val adicionarServicoUseCase: AdicionarServicoUseCase,
    private val removerServicoUseCase: RemoverServicoUseCase,
    private val gerarOrcamentoUseCase: GerarOrcamentoUseCase,
    private val aprovarOrcamentoUseCase: AprovarOrcamentoUseCase,
    private val reprovarOrcamentoUseCase: ReprovarOrcamentoUseCase,
    private val iniciarExecucaoUseCase: IniciarExecucaoUseCase,
    private val concluirItemServicoUseCase: ConcluirItemServicoUseCase,
    private val finalizarUseCase: FinalizarOrdemServicoUseCase,
    private val entregarUseCase: EntregarOrdemServicoUseCase,
    private val cancelarUseCase: CancelarOrdemServicoUseCase,
    private val definirPrazoUseCase: DefinirPrazoUseCase,
    private val reatribuirMecanicoUseCase: ReatribuirMecanicoUseCase,
    private val mapper: OrdemServicoWebMapper,
) : OrdemServicoApiPort {

    override fun abrir(requisicao: AbrirOrdemRequest): ResponseEntity<OrdemServicoResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            mapper.paraResposta(abrir.executar(mapper.paraComando(requisicao)))
        )

    override fun porId(id: String): ResponseEntity<OrdemServicoResponse> =
        ResponseEntity.ok(mapper.paraResposta(consultar.executar(ordem(id))))

    override fun listar(
        clienteId: String?,
        veiculoId: String?,
        mecanicoId: String?,
        status: String?,
        pagina: Int,
        tamanho: Int,
    ): ResponseEntity<PaginaResponse<OrdemServicoResponse>> {
        val consulta = ListarOrdensServicoQuery(
            filtro = FiltroDeOrdens(
                clienteId = clienteId?.let(UsuarioId::de),
                veiculoId = veiculoId?.let(VeiculoId::de),
                mecanicoId = mecanicoId?.let(UsuarioId::de),
                status = status?.let(::statusDe),
            ),
            pagina = pagina,
            tamanho = tamanho,
        )
        return ResponseEntity.ok(mapper.paraResposta(listarOrdens.executar(consulta)))
    }

    override fun historico(id: String): ResponseEntity<List<TransicaoDeStatusResponse>> =
        ResponseEntity.ok(
            consultarHistorico.executar(ConsultarHistoricoQuery(ordem(id))).map { mapper.paraResposta(it) }
        )

    override fun iniciarDiagnostico(id: String): ResponseEntity<OrdemServicoResponse> =
        ok(iniciarDiagnosticoUseCase.executar(IniciarDiagnosticoCommand(ordem(id))))

    override fun adicionarInsumo(id: String, requisicao: AdicionarInsumoRequest): ResponseEntity<OrdemServicoResponse> =
        ok(
            adicionarInsumoUseCase.executar(
                AdicionarInsumoCommand(
                    ordemServicoId = ordem(id),
                    insumoId = InsumoId.de(requisicao.insumoId),
                    quantidade = requisicao.quantidade,
                )
            )
        )

    override fun removerInsumo(id: String, insumoId: String): ResponseEntity<OrdemServicoResponse> =
        ok(removerInsumoUseCase.executar(RemoverInsumoCommand(ordem(id), InsumoId.de(insumoId))))

    override fun adicionarServico(id: String, requisicao: AdicionarServicoRequest): ResponseEntity<OrdemServicoResponse> =
        ok(
            adicionarServicoUseCase.executar(
                AdicionarServicoCommand(
                    ordemServicoId = ordem(id),
                    servicoId = ServicoId.de(requisicao.servicoId),
                    valor = ValorMonetario(requisicao.valor),
                )
            )
        )

    override fun removerServico(id: String, servicoId: String): ResponseEntity<OrdemServicoResponse> =
        ok(removerServicoUseCase.executar(RemoverServicoCommand(ordem(id), ServicoId.de(servicoId))))

    override fun gerarOrcamento(id: String, requisicao: GerarOrcamentoRequest): ResponseEntity<OrdemServicoResponse> =
        ok(
            gerarOrcamentoUseCase.executar(
                GerarOrcamentoCommand(
                    ordemServicoId = ordem(id),
                    custoMaoDeObra = ValorMonetario(requisicao.custoMaoDeObra),
                    custoInsumos = ValorMonetario(requisicao.custoInsumos),
                )
            )
        )

    override fun aprovarOrcamento(id: String): ResponseEntity<OrdemServicoResponse> =
        ok(aprovarOrcamentoUseCase.executar(AprovarOrcamentoCommand(ordem(id))))

    override fun reprovarOrcamento(id: String, requisicao: ReprovarOrcamentoRequest): ResponseEntity<OrdemServicoResponse> =
        ok(reprovarOrcamentoUseCase.executar(ReprovarOrcamentoCommand(ordem(id), requisicao.motivo)))

    override fun iniciarExecucao(id: String): ResponseEntity<OrdemServicoResponse> =
        ok(iniciarExecucaoUseCase.executar(IniciarExecucaoCommand(ordem(id))))

    override fun concluirItemServico(
        id: String,
        itemId: String,
        requisicao: ConcluirItemServicoRequest,
    ): ResponseEntity<OrdemServicoResponse> = ok(
        concluirItemServicoUseCase.executar(
            ConcluirItemServicoCommand(
                ordemServicoId = ordem(id),
                itemId = ItemOrdemServicoId.de(itemId),
                mecanicoId = UsuarioId.de(requisicao.mecanicoId),
                observacao = requisicao.observacao,
            )
        )
    )

    override fun finalizar(id: String): ResponseEntity<OrdemServicoResponse> =
        ok(finalizarUseCase.executar(FinalizarOrdemServicoCommand(ordem(id))))

    override fun entregar(id: String): ResponseEntity<OrdemServicoResponse> =
        ok(entregarUseCase.executar(EntregarOrdemServicoCommand(ordem(id))))

    override fun cancelar(id: String, requisicao: CancelarOrdemRequest?): ResponseEntity<OrdemServicoResponse> =
        ok(cancelarUseCase.executar(CancelarOrdemServicoCommand(ordem(id), requisicao?.motivo)))

    override fun definirPrazo(id: String, requisicao: DefinirPrazoRequest): ResponseEntity<OrdemServicoResponse> =
        ok(definirPrazoUseCase.executar(DefinirPrazoCommand(ordem(id), requisicao.prazoConclusao)))

    override fun reatribuirMecanico(id: String, requisicao: ReatribuirMecanicoRequest): ResponseEntity<OrdemServicoResponse> =
        ok(
            reatribuirMecanicoUseCase.executar(
                ReatribuirMecanicoCommand(ordem(id), UsuarioId.de(requisicao.mecanicoId))
            )
        )

    private fun ok(ordem: com.clau.service_track.ordens.domain.ordemservico.OrdemServico) =
        ResponseEntity.ok(mapper.paraResposta(ordem))

    private fun ordem(id: String) = OrdemServicoId.de(id)

    private fun statusDe(bruto: String): StatusOrdemServicoEnum = StatusOrdemServicoEnum.entries
        .find { it.name == bruto.uppercase() }
        ?: throw IllegalArgumentException("Status '$bruto' não existe. Valores aceitos: ${StatusOrdemServicoEnum.entries.joinToString()}")
}
