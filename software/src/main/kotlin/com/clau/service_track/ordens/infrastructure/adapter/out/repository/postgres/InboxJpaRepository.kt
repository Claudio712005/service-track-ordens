package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.infrastructure.entity.postgres.InboxEntity
import org.springframework.data.jpa.repository.JpaRepository

interface InboxJpaRepository : JpaRepository<InboxEntity, String>
