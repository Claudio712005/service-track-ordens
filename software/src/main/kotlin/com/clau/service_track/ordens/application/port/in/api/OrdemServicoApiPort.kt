package com.clau.service_track.ordens.application.port.`in`.api

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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Ordens de serviço", description = "Abertura, andamento e histórico da ordem de serviço.")
@RequestMapping("/ordens")
interface OrdemServicoApiPort {

    @Operation(
        summary = "Abre uma ordem de serviço",
        description = "A OS nasce em RECEBIDA. Cliente, mecânico e veículo são referências a outro serviço " +
            "e não são validadas aqui: este serviço não lê o banco de ninguém."
    )
    @ApiResponse(responseCode = "201", description = "Ordem aberta")
    @ApiResponse(responseCode = "400", description = "Corpo inválido", content = [])
    @PostMapping
    fun abrir(@Valid @RequestBody requisicao: AbrirOrdemRequest): ResponseEntity<OrdemServicoResponse>

    @Operation(summary = "Consulta uma ordem de serviço pelo identificador")
    @ApiResponse(responseCode = "200", description = "Ordem encontrada")
    @ApiResponse(responseCode = "404", description = "Ordem inexistente", content = [])
    @GetMapping("/{id}")
    fun porId(@PathVariable id: String): ResponseEntity<OrdemServicoResponse>

    @Operation(
        summary = "Lista ordens de serviço",
        description = "Filtros são combinados por E. Sem filtro, devolve todas, da mais recente para a mais antiga."
    )
    @GetMapping
    fun listar(
        @Parameter(description = "Cliente dono do veículo") @RequestParam(required = false) clienteId: String?,
        @Parameter(description = "Veículo atendido") @RequestParam(required = false) veiculoId: String?,
        @Parameter(description = "Mecânico responsável") @RequestParam(required = false) mecanicoId: String?,
        @Parameter(description = "Estado da OS", example = "EM_EXECUCAO") @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") @Min(0) pagina: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) tamanho: Int,
    ): ResponseEntity<PaginaResponse<OrdemServicoResponse>>

    @Operation(
        summary = "Consulta o histórico de estados",
        description = "Uma linha por transição, em ordem cronológica. O estado atual responde onde a OS está; " +
            "o histórico responde por onde passou e por quê."
    )
    @ApiResponse(responseCode = "404", description = "Ordem inexistente", content = [])
    @GetMapping("/{id}/historico")
    fun historico(@PathVariable id: String): ResponseEntity<List<TransicaoDeStatusResponse>>

    @Operation(summary = "Inicia o diagnóstico", description = "Só a partir de RECEBIDA.")
    @ApiResponse(responseCode = "409", description = "Transição inválida para o estado atual", content = [])
    @PostMapping("/{id}/diagnostico")
    fun iniciarDiagnostico(@PathVariable id: String): ResponseEntity<OrdemServicoResponse>

    @Operation(
        summary = "Adiciona um insumo à ordem",
        description = "Só durante o diagnóstico. Repetir o mesmo insumo soma a quantidade em vez de criar outra linha."
    )
    @PostMapping("/{id}/insumos")
    fun adicionarInsumo(
        @PathVariable id: String,
        @Valid @RequestBody requisicao: AdicionarInsumoRequest,
    ): ResponseEntity<OrdemServicoResponse>

    @Operation(summary = "Remove um insumo da ordem", description = "Só durante o diagnóstico.")
    @DeleteMapping("/{id}/insumos/{insumoId}")
    fun removerInsumo(@PathVariable id: String, @PathVariable insumoId: String): ResponseEntity<OrdemServicoResponse>

    @Operation(summary = "Adiciona um serviço à ordem", description = "Só durante o diagnóstico.")
    @PostMapping("/{id}/servicos")
    fun adicionarServico(
        @PathVariable id: String,
        @Valid @RequestBody requisicao: AdicionarServicoRequest,
    ): ResponseEntity<OrdemServicoResponse>

    @Operation(
        summary = "Remove um serviço da ordem",
        description = "Só durante o diagnóstico, e só se o serviço ainda não foi concluído."
    )
    @DeleteMapping("/{id}/servicos/{servicoId}")
    fun removerServico(@PathVariable id: String, @PathVariable servicoId: String): ResponseEntity<OrdemServicoResponse>

    @Operation(
        summary = "Gera o orçamento",
        description = "Leva a OS para AGUARDANDO_APROVACAO. O valor total é derivado dos dois custos."
    )
    @PostMapping("/{id}/orcamento")
    fun gerarOrcamento(
        @PathVariable id: String,
        @Valid @RequestBody requisicao: GerarOrcamentoRequest,
    ): ResponseEntity<OrdemServicoResponse>

    @Operation(
        summary = "Aprova o orçamento e abre a saga de reserva",
        description = "Aprova o orçamento e **não** avança o estado: a OS fica em AGUARDANDO_APROVACAO até a " +
            "saga de reserva confirmar todos os insumos. Ordem sem insumo não precisa de saga e vai direto " +
            "para EM_EXECUCAO. Acompanhe o progresso em `GET /ordens/{id}/saga`."
    )
    @PostMapping("/{id}/orcamento/aprovacao")
    fun aprovarOrcamento(@PathVariable id: String): ResponseEntity<OrdemServicoResponse>

    @Operation(summary = "Reprova o orçamento", description = "Cancela a OS, com o motivo no histórico.")
    @PostMapping("/{id}/orcamento/reprovacao")
    fun reprovarOrcamento(
        @PathVariable id: String,
        @Valid @RequestBody requisicao: ReprovarOrcamentoRequest,
    ): ResponseEntity<OrdemServicoResponse>

    @Operation(
        summary = "Força o início da execução, sem a saga",
        description = "Transição manual de AGUARDANDO_APROVACAO para EM_EXECUCAO, para operação e " +
            "diagnóstico. No caminho normal quem chama isto é o orquestrador, ao receber EstoqueReservado " +
            "de todos os insumos — e exige orçamento aprovado de qualquer forma."
    )
    @PostMapping("/{id}/execucao")
    fun iniciarExecucao(@PathVariable id: String): ResponseEntity<OrdemServicoResponse>

    @Operation(summary = "Conclui um item de serviço", description = "Só com a OS em execução.")
    @PostMapping("/{id}/itens-servico/{itemId}/conclusao")
    fun concluirItemServico(
        @PathVariable id: String,
        @PathVariable itemId: String,
        @Valid @RequestBody requisicao: ConcluirItemServicoRequest,
    ): ResponseEntity<OrdemServicoResponse>

    @Operation(
        summary = "Pede a finalização e abre a saga de consumo",
        description = "A OS **continua** em EM_EXECUCAO até a saga de consumo confirmar todos os insumos. " +
            "Ordem sem insumo vai direto para FINALIZADA. O consumo não tem compensação: se for recusado, a " +
            "saga termina em FALHA e a OS fica em EM_EXECUCAO, de onde pode ser retentada em " +
            "`POST /ordens/{id}/saga/consumo` depois da reposição de estoque, ou cancelada."
    )
    @PostMapping("/{id}/finalizacao")
    fun finalizar(@PathVariable id: String): ResponseEntity<OrdemServicoResponse>

    @Operation(summary = "Entrega o veículo", description = "Só a partir de FINALIZADA.")
    @PostMapping("/{id}/entrega")
    fun entregar(@PathVariable id: String): ResponseEntity<OrdemServicoResponse>

    @Operation(summary = "Cancela a ordem")
    @PostMapping("/{id}/cancelamento")
    fun cancelar(
        @PathVariable id: String,
        @Valid @RequestBody(required = false) requisicao: CancelarOrdemRequest?,
    ): ResponseEntity<OrdemServicoResponse>

    @Operation(summary = "Define o prazo de conclusão", description = "Uma vez só: redefinir é recusado.")
    @PutMapping("/{id}/prazo")
    fun definirPrazo(
        @PathVariable id: String,
        @Valid @RequestBody requisicao: DefinirPrazoRequest,
    ): ResponseEntity<OrdemServicoResponse>

    @Operation(summary = "Reatribui o mecânico responsável")
    @PutMapping("/{id}/mecanico")
    fun reatribuirMecanico(
        @PathVariable id: String,
        @Valid @RequestBody requisicao: ReatribuirMecanicoRequest,
    ): ResponseEntity<OrdemServicoResponse>
}
