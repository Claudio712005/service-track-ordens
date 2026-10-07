package com.clau.service_track.ordens.infrastructure.adapter.out.observabilidade

import com.clau.service_track.ordens.application.port.out.CorrelacaoPort
import com.clau.service_track.ordens.infrastructure.adapter.web.filter.CorrelacaoFilter
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class CorrelacaoMdcAdapter : CorrelacaoPort {

    override fun correlacaoAtual(): String? = MDC.get(CorrelacaoFilter.CHAVE_CORRELACAO)
}
