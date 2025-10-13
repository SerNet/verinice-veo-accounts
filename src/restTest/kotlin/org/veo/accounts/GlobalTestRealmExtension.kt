/**
 * verinice.veo accounts
 * Copyright (C) 2025  Aziz Khalledi
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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import mu.KotlinLogging
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Store
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.JacksonProvider
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.RealmRepresentation
import java.io.File
import java.lang.Runtime
import java.lang.reflect.Field
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID

private val log = KotlinLogging.logger {}

class GlobalTestRealmExtension : BeforeAllCallback {
    companion object {
        @Volatile
        private var testRealmName: String? = null
        private val lock = Any()
        private var cleanedUp = false
        private var shutdownHookAdded = false
    }

    @Suppress("DEPRECATION")
    override fun beforeAll(context: ExtensionContext) {
        synchronized(lock) {
            if (testRealmName != null) return // already started

            testRealmName = createTestRealm()

            if (!shutdownHookAdded) {
                shutdownHookAdded = true
                Runtime.getRuntime().addShutdownHook(Thread(::cleanupTestRealms))
            }
        }
    }

    fun sanitizeRealmExportForImport(realm: RealmRepresentation): RealmRepresentation {
        realm.apply {
            id = null
            groups?.forEach(::clearGroupIds)
            clients?.forEach { it.authenticationFlowBindingOverrides = null }
            identityProviders?.forEach { it.internalId = null }
            components?.values?.flatten()?.forEach { it.id = null }
        }

        val mapper = jacksonObjectMapper()
        val json =
            mapper
                .writeValueAsString(realm)
                .replace(
                    Regex(
                        """"([A-Za-z0-9_]*?)"\s*:\s*"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"""",
                    ),
                ) { """"${it.groupValues[1]}":null""" }

        return mapper.readValue(json, RealmRepresentation::class.java)
    }

    fun createTestRealm(
        masterClientId: String = "veo-accounts-rest-test",
        sourceRealmName: String = "verinice-veo",
        sourceClientId: String = "veo-accounts",
    ): String {
        val testRealmName =
            System.getenv("VEO_ACCOUNTS_KEYCLOAK_REALM")
                ?: System.getProperty(
                    "veo.accounts.keycloak.realm",
                    "rest-test-${UUID.randomUUID().toString().take(8)}",
                )
        if (!testRealmName.startsWith("rest-test")) {
            throw IllegalStateException("Test realm name must start with 'rest-test'")
        }
        val masterServerUrl =
            System.getenv("VEO_ACCOUNTS_KEYCLOAK_SERVERURL")
                ?: System.getProperty("veo.accounts.keycloak.serverUrl", "https://auth.staging.verinice.com/auth/")

        val masterClientSecret =
            System.getenv("VEO_ACCOUNTS_KEYCLOAK_MASTER_CLIENT_SECRET")
                ?: System
                    .getProperty("veo.accounts.keycloak.master.clientSecret", "")
                    .takeIf { it.isNotEmpty() } ?: throw IllegalStateException("Client secret required for Keycloak admin access.")

        val sourceClientSecret =
            System.getenv("VEO_ACCOUNTS_KEYCLOAK_CLIENTS_SERVICE_SECRET")
                ?: System.getProperty("veo.accounts.keycloak.clients.service.secret", "")

        val sourceClientAuthSecret =
            System.getenv("VEO_ACCOUNTS_KEYCLOAK_CLIENTS_AUTH_SECRET")
                ?: System.getProperty("veo.accounts.keycloak.clients.auth.secret", "")

        val keycloakAdminMaster =
            KeycloakBuilder
                .builder()
                .serverUrl(masterServerUrl)
                .realm("master")
                .clientId(masterClientId)
                .clientSecret(masterClientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .resteasyClient(buildClient())
                .build()

        val maxRealms = 50
        val currentRealmCount = keycloakAdminMaster.realms().findAll().size
        if (currentRealmCount > maxRealms) {
            keycloakAdminMaster.close()
            throw IllegalStateException("Too many realms found in Keycloak instance ($currentRealmCount > $maxRealms)")
        }

        val keycloakAdminSource =
            KeycloakBuilder
                .builder()
                .serverUrl(masterServerUrl)
                .realm(sourceRealmName)
                .clientId(sourceClientId)
                .clientSecret(sourceClientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .resteasyClient(buildClient())
                .build()

        try {
            var sourceRealmExport =
                keycloakAdminSource.realm(sourceRealmName).partialExport(true, true).apply {
                    realm = testRealmName
                    displayName = "Test Clone of $sourceRealmName"
                    isEnabled = true
                    components = null
                    authenticationFlows = null
                    authenticatorConfig = null
                }

            sourceRealmExport = sanitizeRealmExportForImport(sourceRealmExport)

            sourceRealmExport.clients.find { it.clientId == "veo-accounts" }?.secret = sourceClientSecret
            sourceRealmExport.clients.find { it.clientId == "veo-development-client" }?.secret = sourceClientAuthSecret

            try {
                keycloakAdminMaster.realms().create(sourceRealmExport)
            } catch (e: jakarta.ws.rs.WebApplicationException) {
                throw IllegalStateException(
                    "Failed to create realm '$testRealmName' (status=${e.response?.status})",
                    e,
                )
            }

            val keycloakAdminTestRealm =
                KeycloakBuilder
                    .builder()
                    .serverUrl(masterServerUrl)
                    .realm(testRealmName)
                    .clientId(sourceClientId)
                    .clientSecret(sourceClientSecret)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .resteasyClient(buildClient())
                    .build()

            createTestLicense(keycloakAdminTestRealm, testRealmName)

            System.getProperties().putAll(
                mapOf(
                    "veo.accounts.keycloak.realm" to testRealmName,
                ),
            )
        } catch (e: Exception) {
            throw e
        } finally {
            keycloakAdminMaster.close()
            keycloakAdminSource.close()
        }
        return testRealmName
    }

    private fun safeDeleteRealm(realmName: String) {
        if (!realmName.startsWith("rest-test")) {
            log.warn { "⚠️ Skipping deletion of protected or invalid realm: $realmName" }
            return
        }

        val serverUrl =
            System.getenv("VEO_ACCOUNTS_KEYCLOAK_SERVERURL")
                ?: System.getProperty("veo.accounts.keycloak.serverUrl", "https://auth.staging.verinice.com/auth/")
        val clientSecret =
            System.getenv("VEO_ACCOUNTS_KEYCLOAK_MASTER_CLIENT_SECRET")
                ?: System.getProperty("veo.accounts.keycloak.master.clientSecret", "")

        log.info { "Deleting test realm: $realmName" }

        KeycloakBuilder
            .builder()
            .serverUrl(serverUrl)
            .realm("master")
            .clientId("veo-accounts-rest-test")
            .clientSecret(clientSecret)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .resteasyClient(buildClient())
            .build()
            .use {
                try {
                    it.realm(realmName).remove()
                    log.info { "✅ Realm $realmName deleted successfully" }
                } catch (e: Exception) {
                    log.error(e) { "❌ Failed to delete realm: $realmName" }
                    throw e
                }
            }
    }

    private fun cleanupTestRealms() =
        synchronized(lock) {
            if (cleanedUp) return@synchronized
            cleanedUp = true
            val realm = testRealmName ?: return log.debug { "No test realm to clean up." }
            try {
                safeDeleteRealm(realm)
            } catch (e: Exception) {
                log.error(e) { "❌ Failed to clean up test realm: $realm" }
            }
        }

    private fun clearGroupIds(group: org.keycloak.representations.idm.GroupRepresentation) {
        group.apply {
            id = null
            realmRoles?.clear()
            clientRoles?.clear()
            subGroups?.forEach { clearGroupIds(it) }
        }
    }

    private fun buildClient(): Client {
        val proxyHost =
            System.getenv("VEO_ACCOUNTS_KEYCLOAK_PROXYHOST")
                ?: System.getProperty("veo.accounts.keycloak.proxyHost")
        val proxyPort =
            (
                System.getenv("VEO_ACCOUNTS_KEYCLOAK_PROXYPORT")
                    ?: System.getProperty("veo.accounts.keycloak.proxyPort", "3128")
            ).toInt()

        return (ClientBuilder.newBuilder() as ResteasyClientBuilder)
            .apply { proxyHost?.let { defaultProxy(it, proxyPort, "http") } }
            .register(JacksonProvider::class.java, 100)
            .build()
    }

    private fun createTestLicense(
        keycloakAdmin: Keycloak,
        realmName: String,
    ) {
        log.info { "📄 Creating test license for realm: $realmName" }

        try {
            val realmResource = keycloakAdmin.realm(realmName)
            val realmRepresentation = realmResource.toRepresentation()

            // Test License with: customerNo: 1, totalClients: 0, totalUnits: 0, totalUsers: 7, validUntil: 2030-12-12
            val testLicense =
                """
-----BEGIN PKCS7-----
MIIF8gYJKoZIhvcNAQcCoIIF4zCCBd8CAQExDzANBglghkgBZQMEAgEFADB2Bgkq
hkiG9w0BBwGgaQRneyJjdXN0b21lck5vIjoiMSIsInRvdGFsQ2xpZW50cyI6MCwi
dG90YWxVbml0cyI6MCwidG90YWxVc2VycyI6NywidmFsaWRVbnRpbCI6IjIwMzAt
MTItMTJUMDA6MDA6MDBaIn0NCqCCAw0wggMJMIIB8aADAgECAhQ/bVT7yzyFDg1t
MAlFg0ckL+hpuTANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDDAlUZXN0IENlcnQw
HhcNMjUwOTA0MDgzMDAzWhcNMjUwOTA1MDgzMDAzWjAUMRIwEAYDVQQDDAlUZXN0
IENlcnQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDGvG7NtETTD73d
h06CRWgydGYRBNNdev2j85ExOocp+ILiKGohHG2TjkHhoHQDJbiE9bhPdobliNBN
915gYXoK//AKifoFECDgEc5S1SSwohukjnnwT/jmL2yZj2KVDbo1PVJdrKKjQacH
tfc7DFdgxzA2YID3kM38Dbs5rAtPaN0Z+uh6vymwJpnY6ah9Hu7PrlWGtLHNX1ga
dtf00f6rjLZMRunOWhIEwCNQeD7zGnHu7r6+bZY8glSBwlYhqTHT4wh8UEJQEnIr
5K8B7o1H42eT8qnwOLlcWKf9oG+Kwk0PMvOIPARDoHVg/eolTOjFNTgvhsb0KkZp
K6gSVkkxAgMBAAGjUzBRMB0GA1UdDgQWBBS+WhE+3IRF4igLyK8BDGDFhVvWTDAf
BgNVHSMEGDAWgBS+WhE+3IRF4igLyK8BDGDFhVvWTDAPBgNVHRMBAf8EBTADAQH/
MA0GCSqGSIb3DQEBCwUAA4IBAQBYmo1/rzv+KtQAyvcZMN1hGA8V/dksQevXdyNT
pRO1PPkXejXQqDMlBe2UWZZ76TSXwc6UfrL1jhq5sTT5/IwJFxl3ZmMd+TxlkbJm
+KjJ7U21L9BU4BLtTVGN7ajKEzZbVvi2FMNWX+4z/fJBRm7c7XNdM8mYXFDThiVU
u+1ur8FczBeh/TmsTOVuFMpPgOKle64fwCi3cVeXZM6GhLIowiCshsA8PFzG7biK
zK3Dhib77XXQ6RuWgamkQlO+KB7Yfa3YTG/nYfXiTdjicQLFZ1DasC1atmZaFOph
sgI09+QvrWWQVAFU3062sxq9Awd2lwOuwUa9FwSqj9FWMQ4JMYICPjCCAjoCAQEw
LDAUMRIwEAYDVQQDDAlUZXN0IENlcnQCFD9tVPvLPIUODW0wCUWDRyQv6Gm5MA0G
CWCGSAFlAwQCAQUAoIHkMBgGCSqGSIb3DQEJAzELBgkqhkiG9w0BBwEwHAYJKoZI
hvcNAQkFMQ8XDTI1MTAwOTE3MTYyOVowLwYJKoZIhvcNAQkEMSIEIJUy7sUdskuI
CLzxCJaRqTuxY1YYrHxT0+5fk63zqfOkMHkGCSqGSIb3DQEJDzFsMGowCwYJYIZI
AWUDBAEqMAsGCWCGSAFlAwQBFjALBglghkgBZQMEAQIwCgYIKoZIhvcNAwcwDgYI
KoZIhvcNAwICAgCAMA0GCCqGSIb3DQMCAgFAMAcGBSsOAwIHMA0GCCqGSIb3DQMC
AgEoMA0GCSqGSIb3DQEBAQUABIIBACVhu2SypfAmOqEyWeONlRfFHR3Bn/FAfo6l
wpH5+wBZeffAbjulOLM2ZPUOhBAVkHU1KK6HpjvWqYPxPZEdJwPAJO1kaJj6Lf3u
nYZLVdE4XGQw2yU6nTxIpxESh46d2rIsuiqkFMtfNnBIc4QcNSYauvtqxOR59CTE
MEKh11PqTjyYXVdFrJBBNWQm1qE+HqOp0JPFxgRcqIgUy6Yq+MYb0HYCyzWrQPyJ
Pl7JIyfKtU/x7fpRMGsza6tfMwAmwtUHoxM51Bo/K5KBcd1wzouFJhDdQdJnM1M7
dz6T0crwp+BfKbjVCAylWo0/CvmQzhmcH9V5Li2EWs5CMD2vmb4=
-----END PKCS7-----
                """.trimIndent()

            realmRepresentation.attributes = realmRepresentation.attributesOrEmpty + mapOf("veo-license" to testLicense)
            realmResource.update(realmRepresentation)

            log.info { "✅ Test license created successfully for realm: $realmName" }
        } catch (e: Exception) {
            log.error(e) { "❌ Failed to create test license for realm: $realmName" }
            throw e
        }
    }
}
