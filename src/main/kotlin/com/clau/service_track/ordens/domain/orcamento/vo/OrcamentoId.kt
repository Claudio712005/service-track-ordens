package com.clau.service_track.ordens.domain.orcamento.vo

import java.util.UUID

@JvmInline
value class OrcamentoId private constructor(val valor: String) {
    companion object {
        fun gerar() = OrcamentoId(UUID.randomUUID().toString())
        fun de(valor: String) = OrcamentoId(valor)
    }
}
