package com.clau.service_track.ordens.domain.saga

enum class EtapaDaSaga {
    RESERVA_DE_INSUMOS,
    LIBERACAO_DE_INSUMOS,
    CONSUMO_DE_INSUMOS,
    COBRANCA,
    ESTORNO_DA_COBRANCA;

    companion object {
        const val TETO_DO_NOME = 20

        fun de(nome: String): EtapaDaSaga = entries.find { it.name == nome }
            ?: throw IllegalArgumentException("Etapa de saga desconhecida: $nome")
    }
}
