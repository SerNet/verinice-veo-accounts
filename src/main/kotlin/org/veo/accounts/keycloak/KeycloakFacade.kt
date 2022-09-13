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
package org.veo.accounts.keycloak

import mu.KotlinLogging.logger
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS
import org.keycloak.admin.client.KeycloakBuilder.builder
import org.keycloak.admin.client.resource.RealmResource
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus
import org.veo.accounts.exceptions.AbstractMappedException
import javax.ws.rs.client.Client
import javax.ws.rs.core.Response

private val log = logger {}

/**
 * Facade for keycloak admin REST client. Do not access Keycloak REST API in production without using this service.
 *
 * Each operation performed through this facade catches and logs exceptions so their details are not leaked to
 * REST API clients.
 */
@Component
class KeycloakFacade(
    @Value("\${veo.accounts.keycloak.serverUrl}")
    private val serverUrl: String,

    @Value("\${veo.accounts.keycloak.realm}")
    private val realmName: String,

    @Value("\${veo.accounts.keycloak.clients.service.name}")
    private val clientId: String,

    @Value("\${veo.accounts.keycloak.clients.service.secret}")
    private val secret: String,

    @Value("\${veo.accounts.keycloak.proxyHost:#{null}}")
    private val proxyHost: String?,

    @Value("\${veo.accounts.keycloak.proxyPort:#{3128}}")
    private val proxyPort: Int
) {
    private val realm = builder()
        .serverUrl(serverUrl)
        .realm(realmName)
        .grantType(CLIENT_CREDENTIALS)
        .clientId(clientId)
        .clientSecret(secret)
        .resteasyClient(buildClient())
        .build()
        .realm(realmName)

    fun <R> perform(action: RealmResource.() -> R): R = try {
        action(realm)
    } catch (ex: AbstractMappedException) {
        // mapped exceptions can pass
        throw ex
    } catch (ex: KeycloakException) {
        // exceptions thrown by this function are also safe
        throw ex
    } catch (ex: Throwable) {
        // everything else may contain secrets and must not pass
        log.error("Unexpected keycloak communication error", ex)
        throw KeycloakException()
    }

    fun parseResourceId(response: Response): String = response
        .getHeaderString("Location")
        ?.substringAfterLast('/')
        ?: throw IllegalStateException("Failed parsing ID")

    private fun buildClient(): Client = ResteasyClientBuilder()
        .apply { proxyHost?.let { defaultProxy(it, proxyPort, "http") } }
        .build()

    /**
     * Exception type for unexpected things that went wrong when talking to keycloak. This is used so no details
     * are leaked to REST API clients.
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    class KeycloakException : Exception()
}
