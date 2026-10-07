package com.clau.service_track.ordens.infrastructure.entity.postgres

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ORDENS_SERVICO", schema = "ORDENS")
class OrdemServicoEntity(

    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "MOTIVO", nullable = false, length = 500)
    var motivo: String,

    @Column(name = "OBSERVACAO", nullable = false)
    var observacao: String,

    @Column(name = "CLIENTE_ID", nullable = false)
    var clienteId: UUID,

    @Column(name = "MECANICO_ID", nullable = false)
    var mecanicoId: UUID,

    @Column(name = "VEICULO_ID", nullable = false)
    var veiculoId: UUID,

    @Column(name = "STATUS", nullable = false, length = 30)
    var status: String,

    @Column(name = "PRAZO_CONCLUSAO")
    var prazoConclusao: OffsetDateTime? = null,

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "ORCAMENTO_ID")
    var orcamento: OrcamentoEntity? = null,

    @OneToMany(mappedBy = "ordemServico", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var itensServico: MutableList<ItemServicoEntity> = mutableListOf(),

    @OneToMany(mappedBy = "ordemServico", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var itensInsumo: MutableList<ItemInsumoEntity> = mutableListOf(),

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_ATUALIZACAO", nullable = false)
    var dataAtualizacao: OffsetDateTime,

    @Version
    @Column(name = "VERSAO", nullable = false)
    var versao: Int = 0,
)
