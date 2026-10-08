package com.clau.service_track.ordens.application.port.out.mensageria

interface RegistroDeMensagemPort {

    fun jaProcessada(tipoDaMensagem: String, chave: String): Boolean

    fun registrarProcessada(tipoDaMensagem: String, chave: String)
}
