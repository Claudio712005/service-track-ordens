package com.clau.service_track.ordens.infrastructure.entity.postgres

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "OUTBOX", schema = "ORDENS")
class OutboxEntity(

    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "AGREGADO_TIPO", nullable = false, length = 40)
    var agregadoTipo: String,

    @Column(name = "AGREGADO_ID", nullable = false)
    var agregadoId: UUID,

    @Column(name = "CHAVE_PARTICAO", nullable = false, length = 60)
    var chaveParticao: String,

    @Column(name = "TIPO_MENSAGEM", nullable = false, length = 60)
    var tipoMensagem: String,

    @Column(name = "VERSAO_MENSAGEM", nullable = false)
    var versaoMensagem: Short = 1,

    @Column(name = "TOPICO", nullable = false, length = 120)
    var topico: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "PAYLOAD", nullable = false)
    var payload: String,

    @Column(name = "TRACEPARENT", length = 64)
    var traceparent: String? = null,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_PUBLICACAO")
    var dataPublicacao: OffsetDateTime? = null,
)
