package com.clau.service_track.ordens.domain.saga.vo

import java.util.UUID

@JvmInline
value class PassoDaSagaId private constructor(val valor: String) {
    companion object {
        fun gerar() = PassoDaSagaId(UUID.randomUUID().toString())

        fun de(valor: String) = PassoDaSagaId(valor)
    }
}
