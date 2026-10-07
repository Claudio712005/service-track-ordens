package com.clau.service_track.ordens.domain.ordemservico.vo

import java.util.UUID

@JvmInline
value class ItemInsumoId private constructor(val valor: String) {
    companion object {
        fun gerar() = ItemInsumoId(UUID.randomUUID().toString())
        fun de(valor: String) = ItemInsumoId(valor)
    }
}
