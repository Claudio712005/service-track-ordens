package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import java.math.BigDecimal

@Schema(name = "GerarOrcamentoRequest", description = "Custos do orcamento. O total e derivado, nao informado.")
data class GerarOrcamentoRequest(

    @get:Schema(description = "Custo da mao de obra.")
    @get:DecimalMin(value = "0", message = "nao pode ser negativo")
    @get:Digits(integer = 10, fraction = 2)
    val custoMaoDeObra: BigDecimal,

    @get:Schema(description = "Custo dos insumos no momento do orcamento, congelado.")
    @get:DecimalMin(value = "0", message = "nao pode ser negativo")
    @get:Digits(integer = 10, fraction = 2)
    val custoInsumos: BigDecimal,
)
