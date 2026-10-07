package com.clau.service_track.ordens.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(name = "AbrirOrdemRequest", description = "Dados de abertura de uma ordem de servico.")
data class AbrirOrdemRequest(

    @get:Schema(description = "Por que o veiculo entrou na oficina, no relato de quem recebeu.")
    @get:NotBlank
    @get:Size(max = 500)
    val motivo: String,

    @get:Schema(description = "Cliente dono do veiculo. Referencia ao servico de usuarios.")
    @get:Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "deve ser um UUID")
    val clienteId: String,

    @get:Schema(description = "Mecanico responsavel inicial. Pode ser reatribuido depois.")
    @get:Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "deve ser um UUID")
    val mecanicoId: String,

    @get:Schema(description = "Veiculo atendido. Referencia ao servico de usuarios.")
    @get:Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "deve ser um UUID")
    val veiculoId: String,

    @get:Schema(description = "Observacao livre do atendimento.")
    @get:Size(max = 2000)
    val observacao: String = "",

    @get:Schema(description = "Prazo prometido ao cliente. Nao e o prazo de etapa da saga.", nullable = true)
    @get:Future
    val prazoConclusao: LocalDateTime? = null,
)
