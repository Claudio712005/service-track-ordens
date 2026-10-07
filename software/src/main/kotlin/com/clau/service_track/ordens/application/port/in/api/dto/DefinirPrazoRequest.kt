package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import java.time.LocalDateTime

@Schema(name = "DefinirPrazoRequest", description = "Prazo de conclusao prometido ao cliente.")
data class DefinirPrazoRequest(

    @get:Schema(description = "Instante do prazo. Definido uma vez; redefinir e recusado.")
    @get:Future
    val prazoConclusao: LocalDateTime,
)
