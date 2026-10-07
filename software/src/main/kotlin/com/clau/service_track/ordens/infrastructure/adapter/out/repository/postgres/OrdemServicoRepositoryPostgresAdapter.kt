package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.application.port.out.repository.FiltroDeOrdens
import com.clau.service_track.ordens.application.port.out.repository.OrdemServicoRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.Pagina
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.infrastructure.adapter.out.mapper.OrdemServicoMapper
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class OrdemServicoRepositoryPostgresAdapter(
    private val ordens: OrdemServicoJpaRepository,
    private val mapper: OrdemServicoMapper,
) : OrdemServicoRepositoryPort {

    @Transactional
    override fun salvar(ordem: OrdemServico): OrdemServico {
        val existente = ordens.findById(UUID.fromString(ordem.id.valor)).orElse(null)
        val entidade = mapper.paraEntidade(ordem, existente)
        return mapper.paraDominio(ordens.save(entidade))
    }

    @Transactional(readOnly = true)
    override fun porId(id: OrdemServicoId): OrdemServico? = ordens
        .findById(UUID.fromString(id.valor))
        .map(mapper::paraDominio)
        .orElse(null)

    @Transactional(readOnly = true)
    override fun listar(filtro: FiltroDeOrdens, pagina: Int, tamanho: Int): Pagina<OrdemServico> {
        val paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "dataCriacao"))

        val resultado = ordens.buscar(
            clienteId = filtro.clienteId?.let { UUID.fromString(it.valor) },
            veiculoId = filtro.veiculoId?.let { UUID.fromString(it.valor) },
            mecanicoId = filtro.mecanicoId?.let { UUID.fromString(it.valor) },
            status = filtro.status?.name,
            paginacao = paginacao,
        )

        return Pagina(
            conteudo = resultado.content.map(mapper::paraDominio),
            pagina = pagina,
            tamanho = tamanho,
            total = resultado.totalElements,
        )
    }
}
