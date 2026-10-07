package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(name = "ItemServicoResponse", description = "Mao de obra contratada nesta OS.")
data class ItemServicoResponse(
    val id: String,
    val servicoId: String,

    @get:Schema(description = "Valor congelado no orcamento desta OS.")
    val valor: BigDecimal,
    val feito: Boolean,
    val mecanicoResponsavelId: String?,
    val observacao: String?,
    val dataRealizacao: LocalDateTime?,
)
