package com.clau.service_track.ordens.domain.ordemservico.vo

import java.util.UUID

@JvmInline
value class ItemOrdemServicoId private constructor(val valor: String) {
    companion object {
        fun gerar() = ItemOrdemServicoId(UUID.randomUUID().toString())
        fun de(valor: String) = ItemOrdemServicoId(valor)
    }
}
