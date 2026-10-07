package com.clau.service_track.ordens.domain.referencia

import java.util.UUID

@JvmInline
value class InsumoId private constructor(val valor: String) {
    companion object {
        fun gerar() = InsumoId(UUID.randomUUID().toString())
        fun de(valor: String) = InsumoId(valor)
    }
}
