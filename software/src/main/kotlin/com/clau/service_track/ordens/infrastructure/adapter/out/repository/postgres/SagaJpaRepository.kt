package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.infrastructure.entity.postgres.SagaEntity
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints

interface SagaJpaRepository : JpaRepository<SagaEntity, UUID> {

    fun findByOrdemServicoIdAndTipo(ordemServicoId: UUID, tipo: String): SagaEntity?

    fun findByOrdemServicoIdAndSituacaoIn(ordemServicoId: UUID, situacoes: Collection<String>): List<SagaEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
        select s from SagaEntity s
        where s.situacao in :situacoes
          and s.prazoDaEtapa < :momento
        order by s.prazoDaEtapa
        """
    )
    fun travarVencidas(situacoes: Collection<String>, momento: OffsetDateTime, limite: Limit): List<SagaEntity>
}
