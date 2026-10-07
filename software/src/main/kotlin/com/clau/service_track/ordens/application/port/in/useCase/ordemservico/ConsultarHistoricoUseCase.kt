package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.application.port.out.repository.TransicaoDeStatus

fun interface ConsultarHistoricoUseCase {

    fun executar(consulta: ConsultarHistoricoQuery): List<TransicaoDeStatus>
}
