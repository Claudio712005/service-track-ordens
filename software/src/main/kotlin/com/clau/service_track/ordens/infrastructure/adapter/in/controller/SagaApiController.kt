package com.clau.service_track.ordens.infrastructure.adapter.`in`.controller

import com.clau.service_track.ordens.application.handler.saga.OrquestradorDaSaga
import com.clau.service_track.ordens.application.port.`in`.api.SagaApiPort
import com.clau.service_track.ordens.application.port.`in`.api.dto.SagaResponse
import com.clau.service_track.ordens.application.port.out.repository.SagaRepositoryPort
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.saga.TipoDeSaga
import com.clau.service_track.ordens.infrastructure.adapter.`in`.mapper.SagaWebMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SagaApiController(
    private val orquestrador: OrquestradorDaSaga,
    private val sagas: SagaRepositoryPort,
    private val mapper: SagaWebMapper,
) : SagaApiPort {

    override fun abrirReserva(id: String): ResponseEntity<SagaResponse> =
        ResponseEntity.ok(mapper.paraResposta(orquestrador.abrirReserva(OrdemServicoId.de(id))))

    override fun abrirConsumo(id: String): ResponseEntity<SagaResponse> =
        ResponseEntity.ok(mapper.paraResposta(orquestrador.abrirConsumo(OrdemServicoId.de(id))))

    override fun consultar(id: String): ResponseEntity<List<SagaResponse>> {
        val ordem = OrdemServicoId.de(id)
        val encontradas = TipoDeSaga.entries.mapNotNull { sagas.porOrdemETipo(ordem, it) }
        return ResponseEntity.ok(encontradas.map(mapper::paraResposta))
    }
}
