package com.clau.service_track.ordens.domain.vo

import com.clau.service_track.ordens.domain.DomainException
import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class ValorMonetario(val valor: BigDecimal) {

    init {
        if (valor < BigDecimal.ZERO) {
            throw DomainException("Valor monetário não pode ser negativo")
        }
    }

    fun somar(outro: ValorMonetario): ValorMonetario {
        return ValorMonetario(this.valor.add(outro.valor))
    }

    fun multiplicar(fator: BigDecimal): ValorMonetario {
        return ValorMonetario(
            this.valor.multiply(fator).setScale(2, RoundingMode.HALF_UP)
        )
    }

    companion object {
        fun zero() = ValorMonetario(BigDecimal.ZERO)
    }
}
