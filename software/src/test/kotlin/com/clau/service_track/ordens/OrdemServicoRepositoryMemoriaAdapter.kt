package com.clau.service_track.ordens

import com.clau.service_track.ordens.application.port.out.repository.FiltroDeOrdens
import com.clau.service_track.ordens.application.port.out.repository.OrdemServicoRepositoryPort
import com.clau.service_track.ordens.application.port.out.repository.Pagina
import com.clau.service_track.ordens.domain.ordemservico.OrdemServico
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import java.util.concurrent.ConcurrentHashMap

class OrdemServicoRepositoryMemoriaAdapter : OrdemServicoRepositoryPort {

    private val ordens = ConcurrentHashMap<String, OrdemServico>()

    var falharNaProximaEscrita: RuntimeException? = null

    fun reiniciar() {
        ordens.clear()
        falharNaProximaEscrita = null
    }

    override fun salvar(ordem: OrdemServico): OrdemServico {
        falharNaProximaEscrita?.let {
            falharNaProximaEscrita = null
            throw it
        }
        ordens[ordem.id.valor] = ordem
        return ordem
    }

    override fun porId(id: OrdemServicoId): OrdemServico? = ordens[id.valor]

    override fun listar(filtro: FiltroDeOrdens, pagina: Int, tamanho: Int): Pagina<OrdemServico> {
        val encontradas = ordens.values
            .filter { filtro.clienteId == null || it.clienteId == filtro.clienteId }
            .filter { filtro.veiculoId == null || it.veiculoId == filtro.veiculoId }
            .filter { filtro.mecanicoId == null || it.obterMecanicoId() == filtro.mecanicoId }
            .filter { filtro.status == null || it.obterStatus() == filtro.status }
            .sortedByDescending { it.dataCriacao }

        return Pagina(
            conteudo = encontradas.drop(pagina * tamanho).take(tamanho),
            pagina = pagina,
            tamanho = tamanho,
            total = encontradas.size.toLong(),
        )
    }
}
