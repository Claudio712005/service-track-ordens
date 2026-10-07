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
@Table(name = "ITENS_SERVICO", schema = "ORDENS")
class ItemServicoEntity(

    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ORDEM_SERVICO_ID", nullable = false)
    var ordemServico: OrdemServicoEntity,

    @Column(name = "SERVICO_ID", nullable = false)
    var servicoId: UUID,

    @Column(name = "VALOR", nullable = false, precision = 12, scale = 2)
    var valor: BigDecimal,

    @Column(name = "FEITO", nullable = false)
    var feito: Boolean,

    @Column(name = "MECANICO_RESPONSAVEL_ID")
    var mecanicoResponsavelId: UUID? = null,

    @Column(name = "OBSERVACAO")
    var observacao: String? = null,

    @Column(name = "DATA_REALIZACAO")
    var dataRealizacao: OffsetDateTime? = null,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_ATUALIZACAO", nullable = false)
    var dataAtualizacao: OffsetDateTime,
)
