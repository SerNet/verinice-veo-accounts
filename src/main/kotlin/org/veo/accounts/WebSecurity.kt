/**
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

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.Duration as Duration1

private val log = KotlinLogging.logger { }

/**
 * This class bundles custom API security configurations.
 */
@Configuration
class WebSecurity(
    @Value("\${veo.cors.origins}")
    private val origins: Array<String>,

    @Value("\${veo.cors.headers}")
    private val allowedHeaders: Array<String>,

    @Value("\${veo.accounts.keycloak.clients.service.name}")
    private val keycloakServiceClientName: String,
) {
    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.cors()
            .and()
            .csrf()
            .disable() // Anonymous access (a user with role "ROLE_ANONYMOUS" must be enabled for
            // swagger-ui. We cannot disable it.
            // Make sure that no critical API can be accessed by an anonymous user!
            // .anonymous()
            //     .disable()
            .authorizeHttpRequests()
            .requestMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/**", "/v2/**")
            .permitAll()
            .requestMatchers(GET)
            .hasRole(Role.READ.roleName)
            .requestMatchers(POST)
            .hasRole(Role.CREATE.roleName)
            .requestMatchers(PUT)
            .hasRole(Role.UPDATE.roleName)
            .requestMatchers(DELETE)
            .hasRole(Role.DELETE.roleName)
            .and()
            .sessionManagement()
            .sessionCreationPolicy(STATELESS)
            .and()
            .oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(
                JwtAuthenticationConverter().apply {
                    setJwtGrantedAuthoritiesConverter { jwt ->
                        jwt.getClaimAsMap("resource_access")
                            ?.get(keycloakServiceClientName)
                            ?.let { it as Map<*, *> }
                            ?.get("roles")
                            ?.let { it as Collection<*> }
                            ?.map { it as String }
                            ?.map { SimpleGrantedAuthority("ROLE_$it") }
                            ?: emptyList()
                    }
                },
            )
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val corsConfig = CorsConfiguration()
        corsConfig.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        // Authorization is always needed, additional headers are configurable:
        corsConfig.addAllowedHeader(AUTHORIZATION)
        corsConfig.addAllowedHeader(CONTENT_TYPE)
        allowedHeaders
            .onEach { log.debug("Added CORS allowed header: $it") }
            .forEach { corsConfig.addAllowedHeader(it) }
        origins
            .onEach { log.debug("Added CORS origin pattern: $it") }
            .forEach { corsConfig.addAllowedOriginPattern(it) }
        corsConfig.setMaxAge(Duration1.ofMinutes(30))
        source.registerCorsConfiguration("/**", corsConfig)
        return source
    }
}
