package com.clau.service_track.ordens.infrastructure.adapter.web.error

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Violacao", description = "Restrição violada em um atributo específico da requisição.")
data class Violacao(

    @get:Schema(description = "Nome do atributo que violou a restrição.", example = "motivo")
    val campo: String,

    @get:Schema(description = "Restrição violada, em texto legível.", example = "não deve estar em branco")
    val mensagem: String,
)
