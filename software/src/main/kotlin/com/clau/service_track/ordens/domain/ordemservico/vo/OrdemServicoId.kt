package com.clau.service_track.ordens.domain.ordemservico.vo

import java.util.UUID

@JvmInline
value class OrdemServicoId private constructor(val valor: String) {
    companion object {
        fun gerar() = OrdemServicoId(UUID.randomUUID().toString())

        fun de(valor: String) = OrdemServicoId(valor)
    }
}
