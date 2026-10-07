package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "OrdemServicoResponse", description = "Estado completo de uma ordem de servico.")
data class OrdemServicoResponse(
    val id: String,
    val motivo: String,
    val observacao: String,
    val clienteId: String,
    val mecanicoId: String,
    val veiculoId: String,

    @get:Schema(description = "Onde a OS esta agora. Por onde passou esta no historico.", example = "AGUARDANDO_APROVACAO")
    val status: String,
    val prazoConclusao: LocalDateTime?,
    val orcamento: OrcamentoResponse?,
    val itensServico: List<ItemServicoResponse>,
    val itensInsumo: List<ItemInsumoResponse>,
    val dataCriacao: LocalDateTime,
    val dataAtualizacao: LocalDateTime,
)
