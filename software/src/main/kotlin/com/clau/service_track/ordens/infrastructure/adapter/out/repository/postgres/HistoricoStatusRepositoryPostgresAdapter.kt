package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.application.port.out.repository.HistoricoStatusRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.TransicaoDeStatus
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.infrastructure.entity.postgres.HistoricoStatusEntity
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class HistoricoStatusRepositoryPostgresAdapter(
    private val historico: HistoricoStatusJpaRepository,
) : HistoricoStatusRepositoryPort {

    @Transactional
    override fun registrar(transicao: TransicaoDeStatus) {
        historico.save(
            HistoricoStatusEntity(
                id = UUID.randomUUID(),
                ordemServicoId = UUID.fromString(transicao.ordemServicoId.valor),
                statusAnterior = transicao.statusAnterior?.name,
                statusNovo = transicao.statusNovo.name,
                motivo = transicao.motivo,
                correlationId = transicao.correlationId,
                dataCriacao = transicao.ocorridoEm,
            )
        )
    }

    @Transactional(readOnly = true)
    override fun porOrdem(id: OrdemServicoId): List<TransicaoDeStatus> = historico
        .findByOrdemServicoIdOrderByDataCriacaoAsc(UUID.fromString(id.valor))
        .map {
            TransicaoDeStatus(
                ordemServicoId = OrdemServicoId.de(it.ordemServicoId.toString()),
                statusAnterior = it.statusAnterior?.let(StatusOrdemServicoEnum::valueOf),
                statusNovo = StatusOrdemServicoEnum.valueOf(it.statusNovo),
                motivo = it.motivo,
                correlationId = it.correlationId,
                ocorridoEm = it.dataCriacao,
            )
        }
}
