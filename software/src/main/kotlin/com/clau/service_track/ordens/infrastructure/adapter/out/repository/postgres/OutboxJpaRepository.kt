package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.infrastructure.entity.postgres.OutboxEntity
import jakarta.persistence.LockModeType
import java.util.UUID
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints

interface OutboxJpaRepository : JpaRepository<OutboxEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select o from OutboxEntity o where o.dataPublicacao is null order by o.dataCriacao")
    fun reservarPendentes(limite: Limit): List<OutboxEntity>

    fun countByDataPublicacaoIsNull(): Long
}
