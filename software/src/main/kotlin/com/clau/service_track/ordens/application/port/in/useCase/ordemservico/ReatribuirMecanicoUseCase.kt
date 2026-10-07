package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.domain.ordemservico.OrdemServico

fun interface ReatribuirMecanicoUseCase {

    fun executar(comando: ReatribuirMecanicoCommand): OrdemServico
}
