package com.caseflow.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun caseFlowOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("CaseFlow API — Solicitações e Conferência Documental")
                    .description("API REST do sistema CaseFlow com regras de conferência automática, controle de versão e papéis Solicitante/Administrador.")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Equipe CaseFlow")
                            .email("contato@caseflow.local")
                    )
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0")
                    )
            )
    }
}
