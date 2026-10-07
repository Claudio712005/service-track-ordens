package com.clau.service_track.ordens.infrastructure.adapter.out.mensageria.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class DadosDeReservarEstoque(
    val insumoId: String,
    val ordemServicoId: String,
    val quantidade: BigDecimal,
    val expiraEm: OffsetDateTime,
)
