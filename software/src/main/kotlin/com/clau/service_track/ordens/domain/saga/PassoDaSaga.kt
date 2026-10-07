package com.clau.service_track.ordens.domain.saga

import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.saga.vo.PassoDaSagaId
import java.math.BigDecimal
import java.time.LocalDateTime

class PassoDaSaga private constructor(
    val id: PassoDaSagaId,
    val etapa: EtapaDaSaga,
    val insumoId: InsumoId,
    val quantidade: BigDecimal,
    situacao: SituacaoDoPasso,
    motivo: String?,
    val dataCriacao: LocalDateTime,
    dataAtualizacao: LocalDateTime,
) {

    var situacao: SituacaoDoPasso = situacao
        private set

    var motivo: String? = motivo
        private set

    var dataAtualizacao: LocalDateTime = dataAtualizacao
        private set

    val pendente: Boolean
        get() = situacao == SituacaoDoPasso.PEDIDO

    companion object {

        fun pedir(etapa: EtapaDaSaga, insumoId: InsumoId, quantidade: BigDecimal): PassoDaSaga {
            if (quantidade <= BigDecimal.ZERO) {
                throw DomainException("Quantidade do passo da saga deve ser maior que zero")
            }
            val agora = LocalDateTime.now()
            return PassoDaSaga(
                id = PassoDaSagaId.gerar(),
                etapa = etapa,
                insumoId = insumoId,
                quantidade = quantidade,
                situacao = SituacaoDoPasso.PEDIDO,
                motivo = null,
                dataCriacao = agora,
                dataAtualizacao = agora,
            )
        }

        fun reconstituir(
            id: PassoDaSagaId,
            etapa: EtapaDaSaga,
            insumoId: InsumoId,
            quantidade: BigDecimal,
            situacao: SituacaoDoPasso,
            motivo: String?,
            dataCriacao: LocalDateTime,
            dataAtualizacao: LocalDateTime,
        ) = PassoDaSaga(id, etapa, insumoId, quantidade, situacao, motivo, dataCriacao, dataAtualizacao)
    }

    fun resolver(nova: SituacaoDoPasso, motivoDaResolucao: String? = null): Boolean {
        if (situacao == nova) return false
        if (!pendente) return false

        situacao = nova
        motivo = motivoDaResolucao
        dataAtualizacao = LocalDateTime.now()
        return true
    }
}
