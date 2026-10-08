package com.clau.service_track.ordens.application.port.`in`.api

import com.clau.service_track.ordens.application.port.`in`.api.dto.SagaResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(
    name = "Saga da ordem de serviço",
    description = "Abertura e acompanhamento da transação distribuída. A OS não ganha estado " +
        "intermediário: o progresso da saga vive aqui."
)
@RequestMapping("/ordens/{id}/saga")
interface SagaApiPort {

    @Operation(
        summary = "Reabre a saga de reserva de insumos",
        description = "No caminho normal quem abre esta saga é `POST /ordens/{id}/orcamento/aprovacao`. " +
            "Esta rota serve para **retentar** depois de uma falha: chamar com a saga em andamento ou já " +
            "encerrada com sucesso devolve a existente sem republicar nada; com a saga em FALHA, incrementa " +
            "a tentativa e republica só os passos que não confirmaram. A tentativa entra na chave de " +
            "idempotência, senão o destino trataria a retentativa como mensagem repetida e a engoliria."
    )
    @ApiResponse(responseCode = "200", description = "Saga aberta ou já existente")
    @ApiResponse(responseCode = "404", description = "Ordem inexistente", content = [])
    @ApiResponse(responseCode = "422", description = "Ordem sem insumo não precisa de saga", content = [])
    @PostMapping("/reserva")
    fun abrirReserva(@PathVariable id: String): ResponseEntity<SagaResponse>

    @Operation(
        summary = "Reabre a saga de consumo de insumos",
        description = "No caminho normal quem abre esta saga é `POST /ordens/{id}/finalizacao`. **Não tem " +
            "compensação**: baixar o reservado é irreversível pelo contrato do catálogo, e por isso o consumo " +
            "é o último passo com efeito externo. É esta rota que retenta o consumo depois de reposição de " +
            "estoque: a saga em FALHA é reaberta com tentativa nova, e o que já foi consumido não é pedido " +
            "de novo."
    )
    @PostMapping("/consumo")
    fun abrirConsumo(@PathVariable id: String): ResponseEntity<SagaResponse>

    @Operation(summary = "Consulta o progresso das sagas da ordem")
    @GetMapping
    fun consultar(@PathVariable id: String): ResponseEntity<List<SagaResponse>>
}
