package com.clau.service_track.ordens.domain.ordemservico

import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.ordemservico.vo.ItemInsumoId
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import java.math.BigDecimal
import java.time.LocalDateTime

class ItemInsumo private constructor(
    val id: ItemInsumoId,
    val insumoId: InsumoId,
    val ordemServicoId: OrdemServicoId,
    var quantidade: BigDecimal,
    val dataCriacao: LocalDateTime,
    var dataAtualizacao: LocalDateTime,
) {

    companion object {

        fun criar(
            insumoId: InsumoId,
            ordemServicoId: OrdemServicoId,
            quantidade: BigDecimal,
        ): ItemInsumo {
            exigirQuantidadePositiva(quantidade)
            val agora = LocalDateTime.now()
            return ItemInsumo(
                id = ItemInsumoId.gerar(),
                insumoId = insumoId,
                ordemServicoId = ordemServicoId,
                quantidade = quantidade,
                dataCriacao = agora,
                dataAtualizacao = agora,
            )
        }

        fun reconstituir(
            id: ItemInsumoId,
            insumoId: InsumoId,
            ordemServicoId: OrdemServicoId,
            quantidade: BigDecimal,
            dataCriacao: LocalDateTime,
            dataAtualizacao: LocalDateTime,
        ): ItemInsumo = ItemInsumo(
            id = id,
            insumoId = insumoId,
            ordemServicoId = ordemServicoId,
            quantidade = quantidade,
            dataCriacao = dataCriacao,
            dataAtualizacao = dataAtualizacao,
        )

        private fun exigirQuantidadePositiva(quantidade: BigDecimal) {
            if (quantidade <= BigDecimal.ZERO) {
                throw DomainException("Quantidade do insumo deve ser maior que zero")
            }
        }
    }

    fun alterarQuantidade(novaQuantidade: BigDecimal) {
        exigirQuantidadePositiva(novaQuantidade)
        quantidade = novaQuantidade
        dataAtualizacao = LocalDateTime.now()
    }

    fun somarQuantidade(adicional: BigDecimal) {
        exigirQuantidadePositiva(adicional)
        quantidade = quantidade.add(adicional)
        dataAtualizacao = LocalDateTime.now()
    }
}
