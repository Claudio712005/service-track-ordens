package com.clau.service_track.ordens.domain.saga

enum class TipoDeSaga(val etapaInicial: EtapaDaSaga, val compensavel: Boolean) {
    RESERVA(EtapaDaSaga.RESERVA_DE_INSUMOS, true),
    CONSUMO(EtapaDaSaga.CONSUMO_DE_INSUMOS, false),
}
