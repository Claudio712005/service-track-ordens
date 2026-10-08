package com.clau.service_track.ordens

import com.clau.service_track.ordens.application.port.out.mensageria.MensagemParaPublicar
import com.clau.service_track.ordens.application.port.out.mensageria.OutboxPort
import java.util.concurrent.CopyOnWriteArrayList

class OutboxMemoriaAdapter : OutboxPort {

    val enfileiradas = CopyOnWriteArrayList<MensagemParaPublicar>()

    fun reiniciar() = enfileiradas.clear()

    override fun enfileirar(mensagens: List<MensagemParaPublicar>) {
        enfileiradas.addAll(mensagens)
    }
}
