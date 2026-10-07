package com.clau.service_track.ordens.application.port.out.repository

data class Pagina<T>(
    val conteudo: List<T>,
    val pagina: Int,
    val tamanho: Int,
    val total: Long,
) {
    val totalDePaginas: Int
        get() = if (tamanho <= 0) 0 else ((total + tamanho - 1) / tamanho).toInt()
}
