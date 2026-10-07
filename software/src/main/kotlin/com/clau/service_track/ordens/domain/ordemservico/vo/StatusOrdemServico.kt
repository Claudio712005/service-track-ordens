package com.clau.service_track.ordens.domain.ordemservico.vo

import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.ordemservico.StatusOrdemServicoEnum

@JvmInline
value class StatusOrdemServico private constructor(
    val valor: StatusOrdemServicoEnum
) {

    companion object {
        fun de(nome: String): StatusOrdemServico {
            val status = StatusOrdemServicoEnum.entries.find { it.name == nome }
                ?: throw DomainException("Status de ordem de serviço desconhecido: $nome")

            return StatusOrdemServico(status)
        }

        fun deEnum(status: StatusOrdemServicoEnum): StatusOrdemServico {
            return StatusOrdemServico(status)
        }
    }

    fun podeTransitarPara(novoStatus: StatusOrdemServicoEnum): Boolean {
        return when (valor) {
            StatusOrdemServicoEnum.RECEBIDA ->
                novoStatus in listOf(
                    StatusOrdemServicoEnum.EM_DIAGNOSTICO,
                    StatusOrdemServicoEnum.CANCELADA
                )

            StatusOrdemServicoEnum.EM_DIAGNOSTICO ->
                novoStatus in listOf(
                    StatusOrdemServicoEnum.AGUARDANDO_APROVACAO,
                    StatusOrdemServicoEnum.CANCELADA
                )

            StatusOrdemServicoEnum.AGUARDANDO_APROVACAO ->
                novoStatus in listOf(
                    StatusOrdemServicoEnum.EM_EXECUCAO,
                    StatusOrdemServicoEnum.CANCELADA
                )

            StatusOrdemServicoEnum.EM_EXECUCAO ->
                novoStatus in listOf(
                    StatusOrdemServicoEnum.FINALIZADA,
                    StatusOrdemServicoEnum.CANCELADA
                )

            StatusOrdemServicoEnum.FINALIZADA ->
                novoStatus == StatusOrdemServicoEnum.ENTREGUE

            else -> false
        }
    }

    fun transitarPara(novoStatus: StatusOrdemServicoEnum): StatusOrdemServico {
        if (!podeTransitarPara(novoStatus)) {
            throw IllegalStateException("Transição inválida de $valor para $novoStatus")
        }
        return StatusOrdemServico(novoStatus)
    }
}
