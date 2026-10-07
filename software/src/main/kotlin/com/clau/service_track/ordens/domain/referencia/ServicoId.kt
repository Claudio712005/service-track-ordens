package com.clau.service_track.ordens.domain.referencia

import java.util.UUID

@JvmInline
value class ServicoId private constructor(val valor: String) {
    companion object {
        fun gerar() = ServicoId(UUID.randomUUID().toString())

        fun de(valor: String) = ServicoId(valor)
    }
}
