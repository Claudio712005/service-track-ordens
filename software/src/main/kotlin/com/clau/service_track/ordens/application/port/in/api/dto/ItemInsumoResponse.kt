package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(name = "ItemInsumoResponse", description = "Insumo que esta OS vai consumir.")
data class ItemInsumoResponse(
    val id: String,
    val insumoId: String,

    @get:Schema(description = "Quantidade na unidade do insumo, que vive no catalogo.")
    val quantidade: BigDecimal,
)
