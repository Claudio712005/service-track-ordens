package com.clau.service_track.ordens.infrastructure.adapter.`in`.mapper

import com.clau.service_track.ordens.application.port.`in`.api.dto.PassoDaSagaResponse
import com.clau.service_track.ordens.application.port.`in`.api.dto.SagaResponse
import com.clau.service_track.ordens.domain.saga.PassoDaSaga
import com.clau.service_track.ordens.domain.saga.Saga
import org.springframework.stereotype.Component

@Component
class SagaWebMapper {

    fun paraResposta(saga: Saga): SagaResponse = SagaResponse(
        id = saga.id.valor,
        ordemServicoId = saga.ordemServicoId.valor,
        tipo = saga.tipo.name,
        situacao = saga.situacao.name,
        etapa = saga.etapa.name,
        prazoDaEtapa = saga.prazoDaEtapa,
        motivo = saga.motivo,
        passos = saga.listarPassos().map(::paraResposta),
    )

    private fun paraResposta(passo: PassoDaSaga): PassoDaSagaResponse = PassoDaSagaResponse(
        insumoId = passo.insumoId.valor,
        etapa = passo.etapa.name,
        quantidade = passo.quantidade,
        situacao = passo.situacao.name,
        motivo = passo.motivo,
    )
}
