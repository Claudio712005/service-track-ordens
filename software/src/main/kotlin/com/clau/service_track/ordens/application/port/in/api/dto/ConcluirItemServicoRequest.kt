package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(name = "ConcluirItemServicoRequest", description = "Conclusao de um item de servico pelo mecanico.")
data class ConcluirItemServicoRequest(

    @get:Schema(description = "Mecanico que executou. Vinculado automaticamente se o item ainda nao tem responsavel.")
    @get:Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "deve ser um UUID")
    val mecanicoId: String,

    @get:Schema(description = "O que foi feito. Obrigatorio.")
    @get:NotBlank
    @get:Size(max = 2000)
    val observacao: String,
)
