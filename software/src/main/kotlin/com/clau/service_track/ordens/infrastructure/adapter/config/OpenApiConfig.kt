package com.clau.service_track.ordens.infrastructure.adapter.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("ServiceTrack — ordens de serviço")
            .version("v1")
            .description(
                "Estado da ordem de serviço e orquestração da saga. Identificadores de cliente, " +
                    "veículo, insumo e serviço são referências opacas a outros serviços: este serviço " +
                    "não lê o banco de ninguém e não valida existência de dado alheio."
            )
    )
}
