package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(name = "OrcamentoResponse", description = "Orcamento da ordem de servico.")
data class OrcamentoResponse(
    val id: String,
    val custoMaoDeObra: BigDecimal,
    val custoInsumos: BigDecimal,

    @get:Schema(description = "Soma dos dois custos. Derivado, nao armazenado.")
    val valorTotal: BigDecimal,

    @get:Schema(description = "Aprovado pelo cliente. Aprovacao nao avanca o estado da OS: quem avanca e a saga.")
    val aprovado: Boolean,

    @get:Schema(description = "Trilha de aprovacao e reprovacao, com motivo.")
    val observacao: String,
    val dataCriacao: LocalDateTime,
    val dataAtualizacao: LocalDateTime,
)
