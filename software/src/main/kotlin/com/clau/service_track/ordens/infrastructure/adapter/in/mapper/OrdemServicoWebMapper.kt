package com.clau.service_track.ordens.infrastructure.adapter.`in`.mapper

import com.clau.service_track.ordens.application.port.`in`.api.dto.AbrirOrdemRequest
import com.clau.service_track.ordens.application.port.`in`.api.dto.ItemInsumoResponse
import com.clau.service_track.ordens.application.port.`in`.api.dto.ItemServicoResponse
import com.clau.service_track.ordens.application.port.`in`.api.dto.OrcamentoResponse
import com.clau.service_track.ordens.application.port.`in`.api.dto.OrdemServicoResponse
import com.clau.service_track.ordens.application.port.`in`.api.dto.PaginaResponse
import com.clau.service_track.ordens.application.port.`in`.api.dto.TransicaoDeStatusResponse
import com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico.AbrirOrdemServicoCommand
import com.clau.service_track.ordens.application.port.out.repository.Pagina
import com.clau.service_track.ordens.application.port.out.repository.TransicaoDeStatus
import com.clau.service_track.ordens.domain.orcamento.Orcamento
import com.clau.service_track.ordens.domain.ordemservico.ItemInsumo
import com.clau.service_track.ordens.domain.ordemservico.ItemOrdemServico
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import org.springframework.stereotype.Component

@Component
class OrdemServicoWebMapper {

    fun paraComando(requisicao: AbrirOrdemRequest) = AbrirOrdemServicoCommand(
        motivo = requisicao.motivo,
        clienteId = UsuarioId.de(requisicao.clienteId),
        mecanicoId = UsuarioId.de(requisicao.mecanicoId),
        veiculoId = VeiculoId.de(requisicao.veiculoId),
        observacao = requisicao.observacao,
        prazoConclusao = requisicao.prazoConclusao,
    )

    fun paraResposta(ordem: OrdemServico): OrdemServicoResponse = OrdemServicoResponse(
        id = ordem.id.valor,
        motivo = ordem.motivo,
        observacao = ordem.observacao,
        clienteId = ordem.clienteId.valor,
        mecanicoId = ordem.obterMecanicoId().valor,
        veiculoId = ordem.veiculoId.valor,
        status = ordem.obterStatus().name,
        prazoConclusao = ordem.obterPrazoConclusao(),
        orcamento = ordem.obterOrcamento()?.let(::paraResposta),
        itensServico = ordem.listarServicos().map(::paraResposta),
        itensInsumo = ordem.listarInsumos().map(::paraResposta),
        dataCriacao = ordem.dataCriacao,
        dataAtualizacao = ordem.dataAtualizacao,
    )

    fun paraResposta(pagina: Pagina<OrdemServico>): PaginaResponse<OrdemServicoResponse> = PaginaResponse(
        conteudo = pagina.conteudo.map { paraResposta(it) },
        pagina = pagina.pagina,
        tamanho = pagina.tamanho,
        total = pagina.total,
        totalDePaginas = pagina.totalDePaginas,
    )

    fun paraResposta(transicao: TransicaoDeStatus): TransicaoDeStatusResponse = TransicaoDeStatusResponse(
        statusAnterior = transicao.statusAnterior?.name,
        statusNovo = transicao.statusNovo.name,
        motivo = transicao.motivo,
        correlationId = transicao.correlationId,
        ocorridoEm = transicao.ocorridoEm,
    )

    private fun paraResposta(orcamento: Orcamento): OrcamentoResponse = OrcamentoResponse(
        id = orcamento.id.valor,
        custoMaoDeObra = orcamento.custoMaoDeObra.valor,
        custoInsumos = orcamento.custoInsumos.valor,
        valorTotal = orcamento.valorTotal.valor,
        aprovado = orcamento.estaAprovado(),
        observacao = orcamento.obterObservacao(),
        dataCriacao = orcamento.dataCriacao,
        dataAtualizacao = orcamento.obterDataAtualizacao(),
    )

    private fun paraResposta(item: ItemOrdemServico): ItemServicoResponse = ItemServicoResponse(
        id = item.id.valor,
        servicoId = item.servicoId.valor,
        valor = item.valor.valor,
        feito = item.feito,
        mecanicoResponsavelId = item.mecanicoResponsavelId?.valor,
        observacao = item.observacao,
        dataRealizacao = item.dataRealizacao,
    )

    private fun paraResposta(item: ItemInsumo): ItemInsumoResponse = ItemInsumoResponse(
        id = item.id.valor,
        insumoId = item.insumoId.valor,
        quantidade = item.quantidade,
    )
}
