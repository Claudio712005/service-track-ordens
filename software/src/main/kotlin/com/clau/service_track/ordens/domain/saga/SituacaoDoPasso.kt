package com.clau.service_track.ordens.domain.saga

enum class SituacaoDoPasso {
    PEDIDO,
    CONFIRMADO,
    RECUSADO,
    EXPIRADO,
    COMPENSADO;

    companion object {
        fun de(nome: String): SituacaoDoPasso = entries.find { it.name == nome }
            ?: throw IllegalArgumentException("Situacao de passo desconhecida: $nome")
    }
}
