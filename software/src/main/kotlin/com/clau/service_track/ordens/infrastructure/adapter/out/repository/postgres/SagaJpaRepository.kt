package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.infrastructure.entity.postgres.SagaEntity
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SagaJpaRepository : JpaRepository<SagaEntity, UUID> {

    fun findByOrdemServicoIdAndTipo(ordemServicoId: UUID, tipo: String): SagaEntity?

    fun findByOrdemServicoIdAndSituacaoIn(ordemServicoId: UUID, situacoes: Collection<String>): List<SagaEntity>

    @Query(
        """
        select s from SagaEntity s
        where s.situacao in :situacoes
          and s.prazoDaEtapa < :momento
        order by s.prazoDaEtapa
        """
    )
    fun comPrazoVencido(situacoes: Collection<String>, momento: OffsetDateTime, limite: Limit): List<SagaEntity>
}
