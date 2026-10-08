package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.application.port.out.repository.SagaRepositoryPort
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.saga.Saga
import com.clau.service_track.ordens.domain.saga.SituacaoDaSaga
import com.clau.service_track.ordens.domain.saga.TipoDeSaga
import com.clau.service_track.ordens.infrastructure.adapter.out.mapper.SagaMapper
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class SagaRepositoryPostgresAdapter(
    private val sagas: SagaJpaRepository,
    private val mapper: SagaMapper,
) : SagaRepositoryPort {

    @Transactional
    override fun salvar(saga: Saga): Saga {
        val existente = sagas.findById(UUID.fromString(saga.id.valor)).orElse(null)
        return mapper.paraDominio(sagas.save(mapper.paraEntidade(saga, existente)))
    }

    @Transactional(readOnly = true)
    override fun porOrdemETipo(ordemServicoId: OrdemServicoId, tipo: TipoDeSaga): Saga? = sagas
        .findByOrdemServicoIdAndTipo(UUID.fromString(ordemServicoId.valor), tipo.name)
        ?.let(mapper::paraDominio)

    @Transactional(readOnly = true)
    override fun emCursoPorOrdem(ordemServicoId: OrdemServicoId): List<Saga> = sagas
        .findByOrdemServicoIdAndSituacaoIn(UUID.fromString(ordemServicoId.valor), SITUACOES_ABERTAS)
        .map(mapper::paraDominio)

    override fun travarProximaVencida(momento: LocalDateTime): Saga? = sagas
        .travarVencidas(
            SITUACOES_ABERTAS,
            momento.atZone(ZoneId.systemDefault()).toOffsetDateTime(),
            Limit.of(1),
        )
        .firstOrNull()
        ?.let(mapper::paraDominio)

    private companion object {
        val SITUACOES_ABERTAS = SituacaoDaSaga.entries.filterNot { it.encerrada }.map { it.name }
    }
}
