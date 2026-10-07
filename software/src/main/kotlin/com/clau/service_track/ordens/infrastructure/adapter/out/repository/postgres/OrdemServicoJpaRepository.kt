package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.infrastructure.entity.postgres.OrdemServicoEntity
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OrdemServicoJpaRepository : JpaRepository<OrdemServicoEntity, UUID> {

    @Query(
        """
        select o from OrdemServicoEntity o
        where (:clienteId is null or o.clienteId = :clienteId)
          and (:veiculoId is null or o.veiculoId = :veiculoId)
          and (:mecanicoId is null or o.mecanicoId = :mecanicoId)
          and (:status is null or o.status = :status)
        """
    )
    fun buscar(
        clienteId: UUID?,
        veiculoId: UUID?,
        mecanicoId: UUID?,
        status: String?,
        paginacao: Pageable,
    ): Page<OrdemServicoEntity>
}
