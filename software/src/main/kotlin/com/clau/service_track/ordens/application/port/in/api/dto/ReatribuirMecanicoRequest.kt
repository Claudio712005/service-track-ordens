package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

@Schema(name = "ReatribuirMecanicoRequest", description = "Troca do mecanico responsavel pela OS.")
data class ReatribuirMecanicoRequest(

    @get:Schema(description = "Novo mecanico responsavel.")
    @get:Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "deve ser um UUID")
    val mecanicoId: String,
)
