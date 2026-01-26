/*
 * verinice.veo accounts
 * Copyright (C) 2022  Jonas Jordan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.veo.accounts

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType.OAUTH2
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.OAuthFlow
import io.swagger.v3.oas.annotations.security.OAuthFlows
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate

const val SECURITY_SCHEME_OAUTH = "OAuth2"
const val SECURITY_SCHEME_CLIENT_INIT_API_KEY = "ClientInitApiKey"

@SpringBootApplication
@SecurityScheme(
    name = SECURITY_SCHEME_OAUTH,
    type = OAUTH2,
    `in` = HEADER,
    description = "openidconnect Login",
    flows =
        OAuthFlows(
            implicit =
                OAuthFlow(
                    authorizationUrl = "\${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/auth",
                ),
        ),
)
@SecurityScheme(
    name = SECURITY_SCHEME_CLIENT_INIT_API_KEY,
    type = APIKEY,
    `in` = HEADER,
    description =
        "Client initialization API key - only subscription services should know this key. " +
            "It is required for creating the initial account for a client. " +
            "The initial user may then authenticate with OAuth2 and create additional accounts in their client.",
)
@OpenAPIDefinition(
    info =
        Info(
            title = "verinice.veo-accounts API",
            description = "OpenAPI documentation for verinice.veo-accounts.",
            license =
                License(
                    name = "GNU Affero General Public License",
                    url = "https://www.gnu.org/licenses/agpl-3.0.html.en",
                ),
            contact =
                Contact(
                    url = "http://verinice.com",
                    email = "verinice@sernet.de",
                ),
        ),
)
@EnableScheduling
class VeoAccountsApplication {
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper =
        ObjectMapper()
            .findAndRegisterModules()
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate = builder.build()
}

fun main(args: Array<String>) {
    runApplication<VeoAccountsApplication>(*args)
}
