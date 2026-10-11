package com.clau.service_track.ordens.infrastructure.adapter.config.mensageria

import com.clau.service_track.ordens.infrastructure.adapter.`in`.mensageria.MensagemInvalidaException
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
@ConditionalOnProperty(prefix = "servicetrack.messaging", name = ["enabled"], havingValue = "true")
class MensageriaConfig {

    @Bean
    fun deadLetterPublishingRecoverer(
        template: KafkaTemplate<String, String>,
        propriedades: MensageriaProperties,
    ) = DeadLetterPublishingRecoverer(template) { registro, _ ->
        org.apache.kafka.common.TopicPartition(propriedades.dltTopic, registro.partition())
    }

    @Bean
    fun sagaListenerContainerFactory(
        consumidores: ConsumerFactory<String, String>,
        recuperador: DeadLetterPublishingRecoverer,
        propriedades: MensageriaProperties,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val espera = ExponentialBackOff(propriedades.initialBackoff.toMillis(), propriedades.backoffMultiplier)
        espera.maxInterval = propriedades.maxBackoff.toMillis()
        espera.maxAttempts = propriedades.attempts.toLong() - 1

        val tratador = DefaultErrorHandler(recuperador, espera)
        tratador.addNotRetryableExceptions(
            MensagemInvalidaException::class.java,
            IllegalArgumentException::class.java,
        )

        val fabrica = ConcurrentKafkaListenerContainerFactory<String, String>()
        fabrica.setConsumerFactory(consumidores)
        fabrica.setConcurrency(propriedades.concurrency)
        fabrica.setCommonErrorHandler(tratador)
        fabrica.containerProperties.isMissingTopicsFatal = false
        fabrica.containerProperties.isObservationEnabled = true
        return fabrica
    }
}
