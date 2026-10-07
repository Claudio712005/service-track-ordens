package com.clau.service_track.ordens.application.port.out.mensageria

import com.clau.service_track.ordens.domain.saga.PassoDaSaga
import com.clau.service_track.ordens.domain.saga.Saga

interface FabricaDeComandoDeEstoquePort {

    fun comandosDe(saga: Saga, passos: List<PassoDaSaga>): List<MensagemParaPublicar>
}
