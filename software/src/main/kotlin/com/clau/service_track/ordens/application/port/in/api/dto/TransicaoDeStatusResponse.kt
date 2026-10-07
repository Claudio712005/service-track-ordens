package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(name = "TransicaoDeStatusResponse", description = "Uma transicao de estado da OS.")
data class TransicaoDeStatusResponse(

    @get:Schema(description = "Nulo na abertura da OS, que nao vem de transicao.", nullable = true)
    val statusAnterior: String?,
    val statusNovo: String,

    @get:Schema(description = "Por que a transicao aconteceu.", nullable = true)
    val motivo: String?,

    @get:Schema(description = "Correlacao da operacao que causou a transicao. Liga esta linha ao log e ao trace.", nullable = true)
    val correlationId: String?,
    val ocorridoEm: OffsetDateTime,
)
