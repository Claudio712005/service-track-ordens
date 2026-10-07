package com.clau.service_track.ordens.application.handler.ordemservico

import com.clau.service_track.ordens.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConsultarHistoricoQuery
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConsultarHistoricoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ConsultarOrdemServicoUseCase
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ListarOrdensServicoQuery
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.ListarOrdensServicoUseCase
import com.clau.service_track.ordens.application.port.out.repository.HistoricoStatusRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.OrdemServicoRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.Pagina
import com.clau.service_track.ordens.application.port.out.repository.TransicaoDeStatus
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrdemServicoQueryHandler(
    private val ordens: OrdemServicoRepositoryPort,
    private val historico: HistoricoStatusRepositoryPort,
) : ConsultarOrdemServicoUseCase, ListarOrdensServicoUseCase, ConsultarHistoricoUseCase {

    @Transactional(readOnly = true)
    override fun executar(id: OrdemServicoId): OrdemServico = ordens.porId(id)
        ?: throw RecursoNaoEncontradoException("Ordem de serviço ${id.valor} não encontrada")

    @Transactional(readOnly = true)
    override fun executar(consulta: ListarOrdensServicoQuery): Pagina<OrdemServico> =
        ordens.listar(consulta.filtro, consulta.pagina, consulta.tamanho)

    @Transactional(readOnly = true)
    override fun executar(consulta: ConsultarHistoricoQuery): List<TransicaoDeStatus> {
        val id = consulta.ordemServicoId
        if (ordens.porId(id) == null) {
            throw RecursoNaoEncontradoException("Ordem de serviço ${id.valor} não encontrada")
        }
        return historico.porOrdem(id)
    }
}
