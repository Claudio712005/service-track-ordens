package com.clau.service_track.ordens.infrastructure.adapter.`in`.mensageria.dto

import java.time.OffsetDateTime
import tools.jackson.databind.JsonNode

data class EnvelopeDeMensagem(
    val idMensagem: String,
    val tipo: String,
    val versao: Short = 1,
    val ocorridoEm: OffsetDateTime,
    val correlationId: String?,
    val traceId: String?,
    val dados: JsonNode,
)
