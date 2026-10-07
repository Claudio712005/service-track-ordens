package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.OrdemServico

fun interface GerarOrcamentoUseCase {

    fun executar(comando: GerarOrcamentoCommand): OrdemServico
}
