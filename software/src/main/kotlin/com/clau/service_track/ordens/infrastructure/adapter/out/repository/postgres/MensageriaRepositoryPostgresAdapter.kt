package com.clau.service_track.ordens.infrastructure.adapter.out.repository.postgres

import com.clau.service_track.ordens.application.port.out.mensageria.MensagemParaPublicar
import com.clau.service_track.ordens.application.port.out.mensageria.OutboxPort
import com.clau.service_track.ordens.application.port.out.mensageria.RegistroDeMensagemPort
import com.clau.service_track.ordens.infrastructure.entity.postgres.InboxEntity
import com.clau.service_track.ordens.infrastructure.entity.postgres.OutboxEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class MensageriaRepositoryPostgresAdapter(
    private val outbox: OutboxJpaRepository,
    private val inbox: InboxJpaRepository,
) : OutboxPort, RegistroDeMensagemPort {

    @Transactional
    override fun enfileirar(mensagens: List<MensagemParaPublicar>) {
        if (mensagens.isEmpty()) return

        outbox.saveAll(
            mensagens.map {
                OutboxEntity(
                    id = UUID.randomUUID(),
                    agregadoTipo = it.agregadoTipo,
                    agregadoId = UUID.fromString(it.agregadoId),
                    chaveParticao = it.chaveDeParticao,
                    tipoMensagem = it.tipoMensagem,
                    versaoMensagem = it.versaoMensagem,
                    topico = it.topico,
                    payload = it.payload,
                    traceparent = it.traceparent,
                    dataCriacao = OffsetDateTime.now(ZoneOffset.UTC),
                )
            }
        )
    }

    @Transactional(readOnly = true)
    override fun jaProcessada(tipoDaMensagem: String, chave: String): Boolean =
        inbox.existsById(identidadeNoInbox(tipoDaMensagem, chave))

    @Transactional
    override fun registrarProcessada(tipoDaMensagem: String, chave: String) {
        inbox.save(
            InboxEntity(
                id = identidadeNoInbox(tipoDaMensagem, chave),
                tipoMensagem = tipoDaMensagem,
                dataProcessamento = OffsetDateTime.now(ZoneOffset.UTC),
            )
        )
    }

    private fun identidadeNoInbox(tipoDaMensagem: String, chave: String) = "$tipoDaMensagem:$chave"
}
