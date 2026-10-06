package com.clau.service_track.ordens.domain.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.vo.ItemOrdemServicoId
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.ServicoId
import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import java.time.LocalDateTime

class ItemOrdemServico private constructor(
    val id: ItemOrdemServicoId,
    val servicoId: ServicoId,
    val ordemServicoId: OrdemServicoId,
    var valor: ValorMonetario,
    var feito: Boolean,
    var mecanicoResponsavelId: UsuarioId?,
    var dataRealizacao: LocalDateTime?,
    var observacao: String?,
    val dataCriacao: LocalDateTime,
    var dataAtualizacao: LocalDateTime,
) {

    companion object {

        fun criar(
            servicoId: ServicoId,
            ordemServicoId: OrdemServicoId,
            valor: ValorMonetario,
        ): ItemOrdemServico {
            val agora = LocalDateTime.now()
            return ItemOrdemServico(
                id = ItemOrdemServicoId.gerar(),
                servicoId = servicoId,
                ordemServicoId = ordemServicoId,
                valor = valor,
                feito = false,
                mecanicoResponsavelId = null,
                dataRealizacao = null,
                observacao = null,
                dataCriacao = agora,
                dataAtualizacao = agora,
            )
        }

        fun reconstituir(
            id: ItemOrdemServicoId,
            servicoId: ServicoId,
            ordemServicoId: OrdemServicoId,
            valor: ValorMonetario,
            feito: Boolean,
            mecanicoResponsavelId: UsuarioId?,
            dataRealizacao: LocalDateTime?,
            observacao: String?,
            dataCriacao: LocalDateTime,
            dataAtualizacao: LocalDateTime,
        ): ItemOrdemServico = ItemOrdemServico(
            id = id,
            servicoId = servicoId,
            ordemServicoId = ordemServicoId,
            valor = valor,
            feito = feito,
            mecanicoResponsavelId = mecanicoResponsavelId,
            dataRealizacao = dataRealizacao,
            observacao = observacao,
            dataCriacao = dataCriacao,
            dataAtualizacao = dataAtualizacao,
        )
    }

    fun atualizarValor(novoValor: ValorMonetario) {
        if (feito) {
            throw DomainException("Não é possível alterar o valor de um serviço já concluído")
        }
        valor = novoValor
        dataAtualizacao = LocalDateTime.now()
    }

    fun vincularMecanico(mecanicoId: UsuarioId) {
        if (feito) {
            throw DomainException("Não é possível vincular mecânico a um serviço já concluído")
        }
        if (mecanicoResponsavelId != null) {
            throw DomainException(
                "Serviço já possui mecânico vinculado. Desvincule o atual antes de vincular outro"
            )
        }
        mecanicoResponsavelId = mecanicoId
        dataAtualizacao = LocalDateTime.now()
    }

    fun desvincularMecanico() {
        if (feito) {
            throw DomainException("Não é possível desvincular mecânico de um serviço já concluído")
        }
        mecanicoResponsavelId = null
        dataAtualizacao = LocalDateTime.now()
    }

    fun concluir(observacaoMecanico: String) {
        if (feito) {
            throw DomainException("Serviço já foi concluído nesta OS")
        }
        if (mecanicoResponsavelId == null) {
            throw DomainException("Não é possível concluir um serviço sem mecânico vinculado")
        }
        if (observacaoMecanico.isBlank()) {
            throw DomainException("Observação do mecânico não pode ser vazia")
        }
        feito = true
        dataRealizacao = LocalDateTime.now()
        observacao = observacaoMecanico
        dataAtualizacao = dataRealizacao!!
    }
}
