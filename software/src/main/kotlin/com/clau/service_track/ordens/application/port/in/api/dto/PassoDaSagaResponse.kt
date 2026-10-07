package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(name = "PassoDaSagaResponse", description = "Um passo da saga: um insumo numa etapa.")
data class PassoDaSagaResponse(
    val insumoId: String,
    val etapa: String,
    val quantidade: BigDecimal,

    @get:Schema(description = "PEDIDO, CONFIRMADO, RECUSADO, EXPIRADO ou COMPENSADO.")
    val situacao: String,

    @get:Schema(description = "Texto que o serviço de destino devolveu na recusa.", nullable = true)
    val motivo: String?,
)
