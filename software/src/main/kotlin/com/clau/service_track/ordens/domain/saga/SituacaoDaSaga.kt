package com.clau.service_track.ordens.domain.saga

enum class SituacaoDaSaga(val encerrada: Boolean) {
    EM_CURSO(false),
    CONCLUIDA(true),
    COMPENSANDO(false),
    COMPENSADA(true),
    FALHA(true);

    companion object {
        fun de(nome: String): SituacaoDaSaga = entries.find { it.name == nome }
            ?: throw IllegalArgumentException("Situacao de saga desconhecida: $nome")
    }
}
