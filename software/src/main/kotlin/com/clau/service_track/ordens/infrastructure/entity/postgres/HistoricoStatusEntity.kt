package com.clau.service_track.ordens.infrastructure.entity.postgres

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "HISTORICO_STATUS", schema = "ORDENS")
class HistoricoStatusEntity(

    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "ORDEM_SERVICO_ID", nullable = false)
    var ordemServicoId: UUID,

    @Column(name = "STATUS_ANTERIOR", length = 30)
    var statusAnterior: String? = null,

    @Column(name = "STATUS_NOVO", nullable = false, length = 30)
    var statusNovo: String,

    @Column(name = "MOTIVO", length = 500)
    var motivo: String? = null,

    @Column(name = "CORRELATION_ID", length = 64)
    var correlationId: String? = null,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,
)
