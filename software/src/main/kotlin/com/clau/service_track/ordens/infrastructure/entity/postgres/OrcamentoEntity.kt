package com.clau.service_track.ordens.infrastructure.entity.postgres

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ORCAMENTOS", schema = "ORDENS")
class OrcamentoEntity(

    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "CUSTO_MAO_DE_OBRA", nullable = false, precision = 12, scale = 2)
    var custoMaoDeObra: BigDecimal,

    @Column(name = "CUSTO_INSUMOS", nullable = false, precision = 12, scale = 2)
    var custoInsumos: BigDecimal,

    @Column(name = "APROVADO", nullable = false)
    var aprovado: Boolean,

    @Column(name = "OBSERVACAO", nullable = false)
    var observacao: String,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_ATUALIZACAO", nullable = false)
    var dataAtualizacao: OffsetDateTime,
)
