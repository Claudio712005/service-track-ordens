package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

@Schema(name = "AdicionarInsumoRequest", description = "Insumo que esta OS vai consumir, com quantidade.")
data class AdicionarInsumoRequest(

    @get:Schema(description = "Insumo no catalogo.")
    @get:Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "deve ser um UUID")
    val insumoId: String,

    @get:Schema(description = "Quantidade na unidade do insumo. E o que vai no comando ReservarEstoque.")
    @get:DecimalMin(value = "0", inclusive = false, message = "deve ser maior que zero")
    @get:Digits(integer = 10, fraction = 4)
    val quantidade: BigDecimal,
)
