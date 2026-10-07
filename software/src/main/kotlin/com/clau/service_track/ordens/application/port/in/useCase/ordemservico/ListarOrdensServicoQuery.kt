package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.application.port.out.repository.FiltroDeOrdens

data class ListarOrdensServicoQuery(
    val filtro: FiltroDeOrdens = FiltroDeOrdens(),
    val pagina: Int = 0,
    val tamanho: Int = 20,
)
