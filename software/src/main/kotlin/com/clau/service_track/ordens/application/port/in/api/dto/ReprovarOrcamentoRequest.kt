package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(name = "ReprovarOrcamentoRequest", description = "Reprovacao do orcamento, que cancela a OS.")
data class ReprovarOrcamentoRequest(

    @get:Schema(description = "Por que o cliente recusou. Obrigatorio: reprovacao sem motivo nao se explica depois.")
    @get:NotBlank
    @get:Size(max = 500)
    val motivo: String,
)
