package com.caseflow.bff.infrastructure.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class DownstreamRestClientConfiguration {
    @Bean
    @Qualifier("authServiceRestClient")
    fun authServiceRestClient(
        builder: RestClient.Builder,
        @Value("\${caseflow.downstream.auth-service-url}") baseUrl: String
    ): RestClient = builder.clone().baseUrl(baseUrl).build()

    @Bean
    @Qualifier("caseServiceRestClient")
    fun caseServiceRestClient(
        builder: RestClient.Builder,
        @Value("\${caseflow.downstream.case-service-url}") baseUrl: String
    ): RestClient = builder.clone().baseUrl(baseUrl).build()
}
