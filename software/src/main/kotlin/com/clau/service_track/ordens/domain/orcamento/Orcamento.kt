package com.clau.service_track.ordens.domain.orcamento

import com.clau.service_track.ordens.domain.orcamento.vo.OrcamentoId
import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import java.time.LocalDateTime

class Orcamento private constructor(
    val id: OrcamentoId,
    val dataCriacao: LocalDateTime,
    private var dataAtualizacao: LocalDateTime,
    val custoMaoDeObra: ValorMonetario,
    val custoInsumos: ValorMonetario,
    private var aprovado: Boolean,
    private var observacao: String
) {

    companion object {

        fun gerar(
            custoMaoDeObra: ValorMonetario,
            custoInsumos: ValorMonetario
        ): Orcamento {
            val agora = LocalDateTime.now()
            return Orcamento(
                id = OrcamentoId.gerar(),
                dataCriacao = agora,
                dataAtualizacao = agora,
                custoMaoDeObra = custoMaoDeObra,
                custoInsumos = custoInsumos,
                aprovado = false,
                observacao = ""
            )
        }

        fun reconstituir(
            id: OrcamentoId,
            dataCriacao: LocalDateTime,
            dataAtualizacao: LocalDateTime,
            custoMaoDeObra: ValorMonetario,
            custoInsumos: ValorMonetario,
            aprovado: Boolean,
            observacao: String,
        ): Orcamento = Orcamento(
            id = id,
            dataCriacao = dataCriacao,
            dataAtualizacao = dataAtualizacao,
            custoMaoDeObra = custoMaoDeObra,
            custoInsumos = custoInsumos,
            aprovado = aprovado,
            observacao = observacao,
        )
    }


    val valorTotal: ValorMonetario
        get() = custoMaoDeObra.somar(custoInsumos)

    fun estaAprovado(): Boolean = aprovado

    fun obterObservacao(): String = observacao

    fun obterDataAtualizacao(): LocalDateTime = dataAtualizacao

    fun aprovar() {
        check(!aprovado) { "Orçamento já foi aprovado" }
        aprovado = true
        dataAtualizacao = LocalDateTime.now()
        observacao += "\nOrçamento aprovado em $dataAtualizacao;"
    }

    fun reprovar(motivo: String) {
        if (motivo.isBlank()) {
            throw DomainException("Motivo para reprovação do orçamento deve ser informado")
        }
        check(!aprovado) { "Orçamento já aprovado não pode ser reprovado" }
        dataAtualizacao = LocalDateTime.now()
        observacao += "\nMotivo da reprovação: $motivo;"
    }
}
