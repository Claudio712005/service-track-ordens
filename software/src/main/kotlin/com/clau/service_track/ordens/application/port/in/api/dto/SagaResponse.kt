package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
    name = "SagaResponse",
    description = "Progresso da transação distribuída. Vive aqui e não no estado da OS: etapa de " +
        "saga é detalhe de orquestração, e o cliente vê apenas o estado da ordem."
)
data class SagaResponse(
    val id: String,
    val ordemServicoId: String,

    @get:Schema(description = "RESERVA ou CONSUMO.")
    val tipo: String,

    @get:Schema(description = "EM_CURSO, CONCLUIDA, COMPENSANDO, COMPENSADA ou FALHA.")
    val situacao: String,

    @get:Schema(description = "Etapa corrente.")
    val etapa: String,

    @get:Schema(description = "Quando esta etapa reprova por tempo. É sempre menor que o prazo da reserva no catálogo.")
    val prazoDaEtapa: LocalDateTime,

    @get:Schema(description = "Por que a saga reprovou, quando reprovou.", nullable = true)
    val motivo: String?,
    val passos: List<PassoDaSagaResponse>,
)
