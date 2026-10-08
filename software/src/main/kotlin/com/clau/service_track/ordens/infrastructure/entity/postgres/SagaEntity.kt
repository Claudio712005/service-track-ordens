package com.clau.service_track.ordens.infrastructure.entity.postgres

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "SAGAS", schema = "ORDENS")
class SagaEntity(

    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "ORDEM_SERVICO_ID", nullable = false)
    var ordemServicoId: UUID,

    @Column(name = "TIPO", nullable = false, length = 20)
    var tipo: String,

    @Column(name = "SITUACAO", nullable = false, length = 20)
    var situacao: String,

    @Column(name = "ETAPA", nullable = false, length = 20)
    var etapa: String,

    @Column(name = "TENTATIVA", nullable = false)
    var tentativa: Int,

    @Column(name = "PRAZO_DA_ETAPA", nullable = false)
    var prazoDaEtapa: OffsetDateTime,

    @Column(name = "MOTIVO", length = 500)
    var motivo: String? = null,

    @OneToMany(mappedBy = "saga", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var passos: MutableList<SagaPassoEntity> = mutableListOf(),

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_ATUALIZACAO", nullable = false)
    var dataAtualizacao: OffsetDateTime,

    @Version
    @Column(name = "VERSAO", nullable = false)
    var versao: Int = 0,
)
