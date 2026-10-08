package com.clau.service_track.ordens.infrastructure.adapter.`in`.mensageria.dto

data class DadosDeEventoDeEstoque(
    val insumoId: String,
    val ordemServicoId: String,
    val sku: String? = null,
    val motivo: String? = null,
)
