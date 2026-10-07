package com.clau.service_track.ordens.application.port.out.repository

import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId

interface OrdemServicoRepositoryPort {

    fun salvar(ordem: OrdemServico): OrdemServico

    fun porId(id: OrdemServicoId): OrdemServico?

    fun listar(filtro: FiltroDeOrdens, pagina: Int, tamanho: Int): Pagina<OrdemServico>
}
