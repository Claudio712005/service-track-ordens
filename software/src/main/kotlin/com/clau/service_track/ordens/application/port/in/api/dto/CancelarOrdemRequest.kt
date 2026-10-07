package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(name = "CancelarOrdemRequest", description = "Cancelamento da OS.")
data class CancelarOrdemRequest(

    @get:Schema(description = "Motivo do cancelamento. Vai para a observacao da OS e para o historico.", nullable = true)
    @get:Size(max = 500)
    val motivo: String? = null,
)
