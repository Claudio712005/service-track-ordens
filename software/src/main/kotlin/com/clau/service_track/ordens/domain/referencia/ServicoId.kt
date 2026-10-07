package com.clau.service_track.ordens.domain.referencia

import java.util.UUID

@JvmInline
value class ServicoId (val valor: String) {
    companion object {
        fun gerar() = ServicoId(UUID.randomUUID().toString())
    }
}
