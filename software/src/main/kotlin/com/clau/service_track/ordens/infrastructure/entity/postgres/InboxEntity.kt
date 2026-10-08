package com.clau.service_track.ordens.infrastructure.entity.postgres

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "INBOX", schema = "ORDENS")
class InboxEntity(

    @Id
    @Column(name = "ID", nullable = false, length = 120)
    var id: String,

    @Column(name = "TIPO_MENSAGEM", nullable = false, length = 60)
    var tipoMensagem: String,

    @Column(name = "DATA_PROCESSAMENTO", nullable = false)
    var dataProcessamento: OffsetDateTime,
)
