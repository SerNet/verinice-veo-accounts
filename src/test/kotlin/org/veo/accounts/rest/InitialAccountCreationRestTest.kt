/**
 * verinice.veo accounts
 * Copyright (C) 2023  Jonas Jordan
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

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.veo.accounts.dtos.VeoClientId
import java.util.UUID.randomUUID

class InitialAccountCreationRestTest : AbstractRestTest() {
    private lateinit var client: VeoClientId

    @BeforeEach
    fun setup() {
        client = createVeoClientGroup()
    }

    @Test
    fun `initial account can be created`() {
        // when the initial account is created in the blank client
        val accountId = post(
            "/initial",
            body = mapOf(
                "clientId" to client.clientId,
                "username" to "$prefix-primus",
                "firstName" to "Primus",
                "lastName" to "Sinus",
                "emailAddress" to "$prefix-ps@initial.test",
                "language" to "la",
            ),
            headers = mapOf(
                "Authorization" to listOf(clientInitApiKey),
            ),
        ).bodyAsMap["id"] as String

        // then it has been saved
        findAccount(accountId)!!.apply {
            username shouldBe "$prefix-primus"
            email shouldBe "$prefix-ps@initial.test"
            firstName shouldBe "Primus"
            lastName shouldBe "Sinus"
            firstAttribute("locale") shouldBe "la"
        }

        // and it has been assigned to the correct groups
        findAccountGroupNames(accountId) shouldContainExactlyInAnyOrder listOf(
            "veo-user",
            "veo-write-access",
            "veo-accountmanagers",
            "veo_client:${client.clientId}",
        )

        // expect trying to create another initial account in the same client to fail
        post(
            "/initial",
            body = mapOf(
                "clientId" to client.clientId,
                "username" to "$prefix-secondary",
                "firstName" to "Secondary",
                "lastName" to "Mary",
                "emailAddress" to "$prefix-sm@initial.test",
            ),
            headers = mapOf(
                "Authorization" to listOf(clientInitApiKey),
            ),
            expectedStatus = 409,
        ).rawBody shouldBe "Target client already contains accounts, cannot create initial account"
    }

    @Test
    fun `client group must exist`() {
        // expect creating initial account for non-existing client group to fail
        post(
            "/initial",
            body = mapOf(
                "clientId" to randomUUID(),
                "username" to "$prefix-primus",
                "firstName" to "Primus",
                "lastName" to "Sinus",
                "emailAddress" to "$prefix-ps@initial.test",
            ),
            headers = mapOf(
                "Authorization" to listOf(clientInitApiKey),
            ),
            expectedStatus = 422,
        ).rawBody shouldBe "Target veo client does not exist"
    }

    @Test
    fun `name conflicts are handled`() {
        // given an existing account in another client
        post(
            "/initial",
            body = mapOf(
                "clientId" to createVeoClientGroup().clientId,
                "username" to "$prefix-kelly",
                "firstName" to "Kelly",
                "lastName" to "Elly",
                "emailAddress" to "$prefix-ke@initial.test",
            ),
            headers = mapOf(
                "Authorization" to listOf(clientInitApiKey),
            ),
        )

        // expect that reusing the existing username for this client's initial user will fail
        post(
            "/initial",
            body = mapOf(
                "clientId" to client.clientId,
                "username" to "$prefix-kelly",
                "firstName" to "Kirk",
                "lastName" to "Elly",
                "emailAddress" to "$prefix-kirk@initial.test",
            ),
            headers = mapOf(
                "Authorization" to listOf(clientInitApiKey),
            ),
            expectedStatus = 409,
        ).rawBody shouldBe "Username or email address already taken"

        // and that reusing the existing email address for this client's initial user will fail
        post(
            "/initial",
            body = mapOf(
                "clientId" to client.clientId,
                "username" to "$prefix-kirke",
                "firstName" to "Kirk",
                "lastName" to "Elly",
                "emailAddress" to "$prefix-ke@initial.test",
            ),
            headers = mapOf(
                "Authorization" to listOf(clientInitApiKey),
            ),
            expectedStatus = 409,
        ).rawBody shouldBe "Username or email address already taken"
    }
}
