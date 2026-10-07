package com.clau.service_track.ordens.infrastructure.adapter.out.mapper

import com.clau.service_track.ordens.domain.orcamento.Orcamento
import com.clau.service_track.ordens.domain.orcamento.vo.OrcamentoId
import com.clau.service_track.ordens.domain.ordemservico.ItemInsumo
import com.clau.service_track.ordens.domain.ordemservico.ItemOrdemServico
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.vo.ItemInsumoId
import com.clau.service_track.ordens.domain.ordemservico.vo.ItemOrdemServicoId
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.ordemservico.vo.PrazoConclusao
import com.clau.service_track.ordens.domain.ordemservico.vo.StatusOrdemServico
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.referencia.ServicoId
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import com.clau.service_track.ordens.infrastructure.entity.postgres.ItemInsumoEntity
import com.clau.service_track.ordens.infrastructure.entity.postgres.ItemServicoEntity
import com.clau.service_track.ordens.infrastructure.entity.postgres.OrcamentoEntity
import com.clau.service_track.ordens.infrastructure.entity.postgres.OrdemServicoEntity
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class OrdemServicoMapper {

    fun paraDominio(entidade: OrdemServicoEntity): OrdemServico = OrdemServico.reconstituir(
        id = OrdemServicoId.de(entidade.id.toString()),
        motivo = entidade.motivo,
        observacao = entidade.observacao,
        clienteId = UsuarioId.de(entidade.clienteId.toString()),
        mecanicoId = UsuarioId.de(entidade.mecanicoId.toString()),
        veiculoId = VeiculoId.de(entidade.veiculoId.toString()),
        dataCriacao = paraLocal(entidade.dataCriacao),
        dataAtualizacao = paraLocal(entidade.dataAtualizacao),
        status = StatusOrdemServico.de(entidade.status),
        prazoConclusao = entidade.prazoConclusao?.let { PrazoConclusao(paraLocal(it)) },
        orcamento = entidade.orcamento?.let(::paraDominio),
        insumos = entidade.itensInsumo.map(::paraDominio).toMutableList(),
        itensServico = entidade.itensServico.map(::paraDominio).toMutableList(),
    )

    fun paraEntidade(ordem: OrdemServico, existente: OrdemServicoEntity?): OrdemServicoEntity {
        val entidade = existente ?: OrdemServicoEntity(
            id = UUID.fromString(ordem.id.valor),
            motivo = ordem.motivo,
            observacao = ordem.observacao,
            clienteId = UUID.fromString(ordem.clienteId.valor),
            mecanicoId = UUID.fromString(ordem.obterMecanicoId().valor),
            veiculoId = UUID.fromString(ordem.veiculoId.valor),
            status = ordem.obterStatus().name,
            dataCriacao = paraOffset(ordem.dataCriacao),
            dataAtualizacao = paraOffset(ordem.dataAtualizacao),
        )

        entidade.observacao = ordem.observacao
        entidade.mecanicoId = UUID.fromString(ordem.obterMecanicoId().valor)
        entidade.status = ordem.obterStatus().name
        entidade.prazoConclusao = ordem.obterPrazoConclusao()?.let(::paraOffset)
        entidade.dataAtualizacao = paraOffset(ordem.dataAtualizacao)
        entidade.orcamento = sincronizarOrcamento(ordem.obterOrcamento(), entidade.orcamento)

        sincronizarItensServico(ordem, entidade)
        sincronizarItensInsumo(ordem, entidade)

        return entidade
    }

    private fun sincronizarOrcamento(orcamento: Orcamento?, existente: OrcamentoEntity?): OrcamentoEntity? {
        if (orcamento == null) return null

        val entidade = existente ?: OrcamentoEntity(
            id = UUID.fromString(orcamento.id.valor),
            custoMaoDeObra = orcamento.custoMaoDeObra.valor,
            custoInsumos = orcamento.custoInsumos.valor,
            aprovado = orcamento.estaAprovado(),
            observacao = orcamento.obterObservacao(),
            dataCriacao = paraOffset(orcamento.dataCriacao),
            dataAtualizacao = paraOffset(orcamento.obterDataAtualizacao()),
        )

        entidade.aprovado = orcamento.estaAprovado()
        entidade.observacao = orcamento.obterObservacao()
        entidade.dataAtualizacao = paraOffset(orcamento.obterDataAtualizacao())
        return entidade
    }

    private fun sincronizarItensServico(ordem: OrdemServico, entidade: OrdemServicoEntity) {
        val porId = entidade.itensServico.associateBy { it.id.toString() }
        val atuais = ordem.listarServicos()

        atuais.forEach { item ->
            val linha = porId[item.id.valor]
            if (linha == null) {
                entidade.itensServico.add(paraEntidade(item, entidade))
            } else {
                linha.valor = item.valor.valor
                linha.feito = item.feito
                linha.mecanicoResponsavelId = item.mecanicoResponsavelId?.let { UUID.fromString(it.valor) }
                linha.observacao = item.observacao
                linha.dataRealizacao = item.dataRealizacao?.let(::paraOffset)
                linha.dataAtualizacao = paraOffset(item.dataAtualizacao)
            }
        }

        val mantidos = atuais.map { it.id.valor }.toSet()
        entidade.itensServico.removeIf { it.id.toString() !in mantidos }
    }

    private fun sincronizarItensInsumo(ordem: OrdemServico, entidade: OrdemServicoEntity) {
        val porId = entidade.itensInsumo.associateBy { it.id.toString() }
        val atuais = ordem.listarInsumos()

        atuais.forEach { item ->
            val linha = porId[item.id.valor]
            if (linha == null) {
                entidade.itensInsumo.add(paraEntidade(item, entidade))
            } else {
                linha.quantidade = item.quantidade
                linha.dataAtualizacao = paraOffset(item.dataAtualizacao)
            }
        }

        val mantidos = atuais.map { it.id.valor }.toSet()
        entidade.itensInsumo.removeIf { it.id.toString() !in mantidos }
    }

    private fun paraDominio(entidade: OrcamentoEntity): Orcamento = Orcamento.reconstituir(
        id = OrcamentoId.de(entidade.id.toString()),
        dataCriacao = paraLocal(entidade.dataCriacao),
        dataAtualizacao = paraLocal(entidade.dataAtualizacao),
        custoMaoDeObra = ValorMonetario(entidade.custoMaoDeObra),
        custoInsumos = ValorMonetario(entidade.custoInsumos),
        aprovado = entidade.aprovado,
        observacao = entidade.observacao,
    )

    private fun paraDominio(entidade: ItemServicoEntity): ItemOrdemServico = ItemOrdemServico.reconstituir(
        id = ItemOrdemServicoId.de(entidade.id.toString()),
        servicoId = ServicoId.de(entidade.servicoId.toString()),
        ordemServicoId = OrdemServicoId.de(entidade.ordemServico.id.toString()),
        valor = ValorMonetario(entidade.valor),
        feito = entidade.feito,
        mecanicoResponsavelId = entidade.mecanicoResponsavelId?.let { UsuarioId.de(it.toString()) },
        dataRealizacao = entidade.dataRealizacao?.let(::paraLocal),
        observacao = entidade.observacao,
        dataCriacao = paraLocal(entidade.dataCriacao),
        dataAtualizacao = paraLocal(entidade.dataAtualizacao),
    )

    private fun paraDominio(entidade: ItemInsumoEntity): ItemInsumo = ItemInsumo.reconstituir(
        id = ItemInsumoId.de(entidade.id.toString()),
        insumoId = InsumoId.de(entidade.insumoId.toString()),
        ordemServicoId = OrdemServicoId.de(entidade.ordemServico.id.toString()),
        quantidade = entidade.quantidade,
        dataCriacao = paraLocal(entidade.dataCriacao),
        dataAtualizacao = paraLocal(entidade.dataAtualizacao),
    )

    private fun paraEntidade(item: ItemOrdemServico, ordem: OrdemServicoEntity) = ItemServicoEntity(
        id = UUID.fromString(item.id.valor),
        ordemServico = ordem,
        servicoId = UUID.fromString(item.servicoId.valor),
        valor = item.valor.valor,
        feito = item.feito,
        mecanicoResponsavelId = item.mecanicoResponsavelId?.let { UUID.fromString(it.valor) },
        observacao = item.observacao,
        dataRealizacao = item.dataRealizacao?.let(::paraOffset),
        dataCriacao = paraOffset(item.dataCriacao),
        dataAtualizacao = paraOffset(item.dataAtualizacao),
    )

    private fun paraEntidade(item: ItemInsumo, ordem: OrdemServicoEntity) = ItemInsumoEntity(
        id = UUID.fromString(item.id.valor),
        ordemServico = ordem,
        insumoId = UUID.fromString(item.insumoId.valor),
        quantidade = item.quantidade,
        dataCriacao = paraOffset(item.dataCriacao),
        dataAtualizacao = paraOffset(item.dataAtualizacao),
    )

    private fun paraLocal(momento: OffsetDateTime): LocalDateTime =
        momento.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()

    private fun paraOffset(momento: LocalDateTime): OffsetDateTime =
        momento.atZone(ZoneId.systemDefault()).toOffsetDateTime()
}
