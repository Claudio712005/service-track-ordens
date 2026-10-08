package com.clau.service_track.ordens.infrastructure.entity.postgres

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "SAGA_PASSOS", schema = "ORDENS")
class SagaPassoEntity(

    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SAGA_ID", nullable = false)
    var saga: SagaEntity,

    @Column(name = "ETAPA", nullable = false, length = 20)
    var etapa: String,

    @Column(name = "INSUMO_ID", nullable = false)
    var insumoId: UUID,

    @Column(name = "QUANTIDADE", nullable = false, precision = 14, scale = 4)
    var quantidade: BigDecimal,

    @Column(name = "SITUACAO", nullable = false, length = 20)
    var situacao: String,

    @Column(name = "MOTIVO", length = 500)
    var motivo: String? = null,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_ATUALIZACAO", nullable = false)
    var dataAtualizacao: OffsetDateTime,
)
