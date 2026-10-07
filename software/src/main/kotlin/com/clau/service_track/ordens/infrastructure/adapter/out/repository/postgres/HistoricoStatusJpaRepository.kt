package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.infrastructure.entity.postgres.HistoricoStatusEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface HistoricoStatusJpaRepository : JpaRepository<HistoricoStatusEntity, UUID> {

    fun findByOrdemServicoIdOrderByDataCriacaoAsc(ordemServicoId: UUID): List<HistoricoStatusEntity>
}
