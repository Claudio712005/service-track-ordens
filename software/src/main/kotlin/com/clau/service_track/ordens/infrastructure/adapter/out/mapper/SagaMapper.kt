package com.clau.service_track.ordens.infrastructure.adapter.out.mapper

import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.saga.EtapaDaSaga
import com.clau.service_track.ordens.domain.saga.PassoDaSaga
import com.clau.service_track.ordens.domain.saga.Saga
import com.clau.service_track.ordens.domain.saga.SituacaoDaSaga
import com.clau.service_track.ordens.domain.saga.SituacaoDoPasso
import com.clau.service_track.ordens.domain.saga.TipoDeSaga
import com.clau.service_track.ordens.domain.saga.vo.PassoDaSagaId
import com.clau.service_track.ordens.domain.saga.vo.SagaId
import com.clau.service_track.ordens.infrastructure.entity.postgres.SagaEntity
import com.clau.service_track.ordens.infrastructure.entity.postgres.SagaPassoEntity
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class SagaMapper {

    fun paraDominio(entidade: SagaEntity): Saga = Saga.reconstituir(
        id = SagaId.de(entidade.id.toString()),
        ordemServicoId = OrdemServicoId.de(entidade.ordemServicoId.toString()),
        tipo = TipoDeSaga.valueOf(entidade.tipo),
        situacao = SituacaoDaSaga.de(entidade.situacao),
        etapa = EtapaDaSaga.de(entidade.etapa),
        prazoDaEtapa = paraLocal(entidade.prazoDaEtapa),
        motivo = entidade.motivo,
        passos = entidade.passos.map(::paraDominio).toMutableList(),
        dataCriacao = paraLocal(entidade.dataCriacao),
        dataAtualizacao = paraLocal(entidade.dataAtualizacao),
    )

    fun paraEntidade(saga: Saga, existente: SagaEntity?): SagaEntity {
        val entidade = existente ?: SagaEntity(
            id = UUID.fromString(saga.id.valor),
            ordemServicoId = UUID.fromString(saga.ordemServicoId.valor),
            tipo = saga.tipo.name,
            situacao = saga.situacao.name,
            etapa = saga.etapa.name,
            prazoDaEtapa = paraOffset(saga.prazoDaEtapa),
            dataCriacao = paraOffset(saga.dataCriacao),
            dataAtualizacao = paraOffset(saga.dataAtualizacao),
        )

        entidade.situacao = saga.situacao.name
        entidade.etapa = saga.etapa.name
        entidade.prazoDaEtapa = paraOffset(saga.prazoDaEtapa)
        entidade.motivo = saga.motivo
        entidade.dataAtualizacao = paraOffset(saga.dataAtualizacao)

        val porId = entidade.passos.associateBy { it.id.toString() }
        saga.listarPassos().forEach { passo ->
            val linha = porId[passo.id.valor]
            if (linha == null) {
                entidade.passos.add(paraEntidade(passo, entidade))
            } else {
                linha.situacao = passo.situacao.name
                linha.motivo = passo.motivo
                linha.dataAtualizacao = paraOffset(passo.dataAtualizacao)
            }
        }

        return entidade
    }

    private fun paraDominio(entidade: SagaPassoEntity): PassoDaSaga = PassoDaSaga.reconstituir(
        id = PassoDaSagaId.de(entidade.id.toString()),
        etapa = EtapaDaSaga.de(entidade.etapa),
        insumoId = InsumoId.de(entidade.insumoId.toString()),
        quantidade = entidade.quantidade,
        situacao = SituacaoDoPasso.de(entidade.situacao),
        motivo = entidade.motivo,
        dataCriacao = paraLocal(entidade.dataCriacao),
        dataAtualizacao = paraLocal(entidade.dataAtualizacao),
    )

    private fun paraEntidade(passo: PassoDaSaga, saga: SagaEntity) = SagaPassoEntity(
        id = UUID.fromString(passo.id.valor),
        saga = saga,
        etapa = passo.etapa.name,
        insumoId = UUID.fromString(passo.insumoId.valor),
        quantidade = passo.quantidade,
        situacao = passo.situacao.name,
        motivo = passo.motivo,
        dataCriacao = paraOffset(passo.dataCriacao),
        dataAtualizacao = paraOffset(passo.dataAtualizacao),
    )

    private fun paraLocal(momento: OffsetDateTime): LocalDateTime =
        momento.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()

    private fun paraOffset(momento: LocalDateTime): OffsetDateTime =
        momento.atZone(ZoneId.systemDefault()).toOffsetDateTime()
}
