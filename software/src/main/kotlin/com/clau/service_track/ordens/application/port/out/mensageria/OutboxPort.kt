package com.clau.service_track.ordens.application.port.out.mensageria

interface OutboxPort {

    fun enfileirar(mensagens: List<MensagemParaPublicar>)
}
