package com.clau.service_track.ordens.domain.saga.vo

import java.util.UUID

@JvmInline
value class SagaId private constructor(val valor: String) {
    companion object {
        fun gerar() = SagaId(UUID.randomUUID().toString())

        fun de(valor: String) = SagaId(valor)
    }
}
