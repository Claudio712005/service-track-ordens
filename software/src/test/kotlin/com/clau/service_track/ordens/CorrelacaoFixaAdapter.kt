package com.clau.service_track.ordens

import com.clau.service_track.ordens.application.port.out.CorrelacaoPort

class CorrelacaoFixaAdapter(private val correlacao: String?) : CorrelacaoPort {

    override fun correlacaoAtual(): String? = correlacao
}
