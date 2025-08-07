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
package org.veo.accounts.rest

import org.apache.http.HttpHost
import org.apache.http.impl.client.HttpClientBuilder
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.Configuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@Profile("resttest")
internal class TestAuthenticator(
    @Value("\${veo.accounts.keycloak.serverUrl}")
    private val keycloakUrl: String,
    @Value("\${veo.accounts.keycloak.realm}")
    private val realm: String,
    @Value("\${veo.accounts.keycloak.clients.auth.name}")
    private val clientName: String,
    @Value("\${veo.accounts.keycloak.clients.auth.secret}")
    private val clientSecret: String,
    @Value("\${veo.accounts.keycloak.proxyHost:#{null}}")
    private val proxyHost: String?,
    @Value("\${veo.accounts.keycloak.proxyPort:#{3128}}")
    private val proxyPort: Int,
) {
    private val userTokenCache = ConcurrentHashMap<String, String>()

    fun getToken(
        username: String,
        password: String,
    ): String =
        userTokenCache.computeIfAbsent(username) {
            HttpClientBuilder
                .create()
                .apply { proxyHost?.let { setProxy(HttpHost(it, proxyPort)) } }
                .build()
                .use {
                    AuthzClient
                        .create(Configuration(keycloakUrl, realm, clientName, mapOf("secret" to clientSecret), it))
                        .obtainAccessToken(username, password)
                        .token
                }
        }
}
