package com.clau.service_track.ordens.infrastructure.adapter.web.error

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(
    name = "ErrorResponse",
    description = "Corpo devolvido em toda resposta de erro deste serviço. O campo code é " +
        "estável e destinado a tratamento programático; message é destinado a leitura humana " +
        "e pode ser reescrito sem aviso."
)
data class ErrorResponse(

    @get:Schema(description = "Momento em que o erro foi produzido pelo servidor, com deslocamento de fuso.")
    val timestamp: OffsetDateTime,

    @get:Schema(description = "Código de estado HTTP.", example = "400")
    val status: Int,

    @get:Schema(description = "Frase associada ao código de estado.", example = "Bad Request")
    val error: String,

    @get:Schema(
        description = "Classificação estável do erro. Use este campo para decidir fluxo no cliente.",
        example = "REQUISICAO_INVALIDA"
    )
    val code: CodigoErro,

    @get:Schema(description = "Explicação legível. Nunca contém detalhe de implementação nem dado sensível.")
    val message: String,

    @get:Schema(description = "Caminho que originou o erro.", example = "/ordens")
    val path: String,

    @get:Schema(
        description = "Identificador do trace distribuído correspondente. Informe este valor ao " +
            "reportar o problema: ele localiza a requisição inteira, atravessando os demais serviços.",
        nullable = true
    )
    val traceId: String?,

    @get:Schema(description = "Violações por atributo. Presente apenas em erros de validação.", nullable = true)
    val violacoes: List<Violacao>? = null,
)
