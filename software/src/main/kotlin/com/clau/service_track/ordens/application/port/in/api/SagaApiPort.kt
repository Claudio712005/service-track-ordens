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
        summary = "Abre a saga de reserva de insumos",
        description = "Chamada depois da aprovação do orçamento. Publica um `ReservarEstoque` por insumo e, " +
            "quando todos confirmarem, leva a OS para EM_EXECUCAO. Uma recusa entre eles libera as reservas " +
            "que deram certo e cancela a OS. Idempotente: chamar duas vezes devolve a saga existente."
    )
    @ApiResponse(responseCode = "200", description = "Saga aberta ou já existente")
    @ApiResponse(responseCode = "404", description = "Ordem inexistente", content = [])
    @ApiResponse(responseCode = "422", description = "Ordem sem insumo não precisa de saga", content = [])
    @PostMapping("/reserva")
    fun abrirReserva(@PathVariable id: String): ResponseEntity<SagaResponse>

    @Operation(
        summary = "Abre a saga de consumo de insumos",
        description = "Chamada no pedido de finalização. Publica um `ConsumirReserva` por insumo e, quando " +
            "todos confirmarem, leva a OS para FINALIZADA. **Não tem compensação**: baixar o reservado é " +
            "irreversível pelo contrato do catálogo, e por isso o consumo é o último passo com efeito externo."
    )
    @PostMapping("/consumo")
    fun abrirConsumo(@PathVariable id: String): ResponseEntity<SagaResponse>

    @Operation(summary = "Consulta o progresso das sagas da ordem")
    @GetMapping
    fun consultar(@PathVariable id: String): ResponseEntity<List<SagaResponse>>
}
