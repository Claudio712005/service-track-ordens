package com.clau.service_track.ordens.application.port.out.mensageria

data class MensagemParaPublicar(
    val idMensagem: String,
    val agregadoTipo: String,
    val agregadoId: String,
    val chaveDeParticao: String,
    val tipoMensagem: String,
    val versaoMensagem: Short,
    val topico: String,
    val payload: String,
    val traceparent: String?,
)
