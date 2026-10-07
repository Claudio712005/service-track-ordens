package com.clau.service_track.ordens.domain.referencia

import com.clau.service_track.ordens.domain.DomainException
import java.util.UUID

@JvmInline
value class UsuarioId private constructor(val valor: String) {

    init {
        if (!FORMATO_UUID.matches(valor)) {
            throw DomainException("ID de usuário inválido")
        }
    }

    companion object {
        private val FORMATO_UUID =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        fun gerar() = UsuarioId(UUID.randomUUID().toString())

        fun de(valor: String) = UsuarioId(valor)
    }
}
