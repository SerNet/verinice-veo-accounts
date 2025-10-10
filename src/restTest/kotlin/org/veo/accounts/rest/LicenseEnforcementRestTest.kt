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
package org.veo.accounts.rest

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.keycloak.KeycloakFacade

class LicenseEnforcementRestTest : AbstractRestTest() {
    @Autowired
    private lateinit var facade: KeycloakFacade

    private lateinit var client: VeoClientId
    private lateinit var managerId: String
    private var previousLicense: String? = null

    @BeforeEach
    fun setup() {
        facade.perform {
            toRepresentation().let { realm ->
                if (realm.realm != "verinice-veo") {
                    previousLicense = realm.attributes["veo-license"]
                    realm.attributes = realm.attributesOrEmpty - "veo-license"
                    update(realm)
                }
            }
        }

        client = createVeoClientGroup()
        managerId = createManager(client)
    }

    @AfterEach
    fun restoreLicenseAfterEach() {
        facade.perform {
            toRepresentation().let { realm ->
                if (realm.realm != "verinice-veo" && previousLicense != null) {
                    realm.attributes = realm.attributesOrEmpty + mapOf("veo-license" to previousLicense!!)
                    update(realm)
                }
            }
        }
    }

    @Test
    fun `initial account creation is forbidden without installed license`() {
        post(
            "/initial",
            body =
                mapOf(
                    "clientId" to client.clientId,
                    "username" to "$prefix-initial",
                    "firstName" to "Init",
                    "lastName" to "User",
                    "emailAddress" to "$prefix-initial@test.test",
                    "language" to "en",
                ),
            headers = mapOf("Authorization" to listOf(clientInitApiKey)),
            expectedStatus = 403,
        ).rawBody shouldBe "No veo license installed. Please install a license key first."
    }

    @Test
    fun `regular account creation is forbidden without installed license`() {
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-hans",
                "emailAddress" to "$prefix-hans@test.test",
                "firstName" to "Hans",
                "lastName" to "Dance",
                "groups" to listOf("veo-write-access"),
                "enabled" to true,
                "language" to "de",
            ),
            expectedStatus = 403,
        ).rawBody shouldBe "No veo license installed. Please install a license key first."
    }
}
