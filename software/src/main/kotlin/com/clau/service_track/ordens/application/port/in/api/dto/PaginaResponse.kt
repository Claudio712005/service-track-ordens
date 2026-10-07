package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "PaginaResponse", description = "Pagina de resultados.")
data class PaginaResponse<T>(
    val conteudo: List<T>,
    val pagina: Int,
    val tamanho: Int,
    val total: Long,
    val totalDePaginas: Int,
)
