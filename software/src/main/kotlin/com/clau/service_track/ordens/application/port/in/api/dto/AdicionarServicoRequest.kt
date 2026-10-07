package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

@Schema(name = "AdicionarServicoRequest", description = "Servico de mao de obra contratado nesta OS.")
data class AdicionarServicoRequest(

    @get:Schema(description = "Servico no catalogo.")
    @get:Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "deve ser um UUID")
    val servicoId: String,

    @get:Schema(description = "Valor cobrado nesta OS. Fica congelado: o preco de referencia do catalogo pode mudar.")
    @get:DecimalMin(value = "0", message = "nao pode ser negativo")
    @get:Digits(integer = 10, fraction = 2)
    val valor: BigDecimal,
)
