package com.clau.service_track.ordens.application.port.`in`.useCase.ordemservico

import com.clau.service_track.ordens.application.port.out.repository.Pagina
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico

fun interface ListarOrdensServicoUseCase {

    fun executar(consulta: ListarOrdensServicoQuery): Pagina<OrdemServico>
}
