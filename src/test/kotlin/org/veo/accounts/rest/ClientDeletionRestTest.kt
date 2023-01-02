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

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.veo.accounts.auth.VeoClient

class ClientDeletionRestTest : AbstractRestTest() {
    lateinit var client: VeoClient
    lateinit var managerId: String

    lateinit var otherClient: VeoClient
    lateinit var otherManagerId: String

    @BeforeEach
    fun setup() {
        client = createVeoClientGroup()
        managerId = createManager(client)

        otherClient = createVeoClientGroup()
        otherManagerId = createManager(otherClient)
    }

    @Test
    fun `deletes client`() {
        // given some accounts within the client
        val clientAccount1Id = post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-dan",
                "emailAddress" to "$prefix-dan@test.test",
                "firstName" to "Dan",
                "lastName" to "Man",
                "groups" to listOf("veo-write-access"),
                "enabled" to true,
            ),
        ).bodyAsMap["id"] as String
        val clientAccount2Id = post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-syd",
                "emailAddress" to "$prefix-syd@test.test",
                "firstName" to "Syd",
                "lastName" to "Did",
                "groups" to listOf("veo-write-access"),
                "enabled" to true,
            ),
        ).bodyAsMap["id"] as String

        // and an account in the other client
        val otherClientAccountId = post(
            "/",
            otherManagerId,
            mapOf(
                "username" to "$prefix-kim",
                "emailAddress" to "$prefix-kim@test.test",
                "firstName" to "Kim",
                "lastName" to "Dim",
                "groups" to listOf("veo-write-access"),
                "enabled" to true,
            ),
        ).bodyAsMap["id"] as String

        // expect the clients to be populated
        accountExists(managerId) shouldBe true
        accountExists(clientAccount1Id) shouldBe true
        accountExists(clientAccount2Id) shouldBe true
        accountExists(otherManagerId) shouldBe true
        accountExists(otherClientAccountId) shouldBe true

        // when deleting the main client
        sendMessage(
            "client_change",
            mapOf(
                "eventType" to "client_change",
                "clientId" to client.clientId,
                "type" to "DELETION",
            ),
        )

        // then the accounts are gone
        accountExists(managerId) shouldBe false
        accountExists(clientAccount1Id) shouldBe false
        accountExists(clientAccount2Id) shouldBe false

        // and the other client's accounts are still there
        accountExists(otherManagerId) shouldBe true
        accountExists(otherClientAccountId) shouldBe true
    }
}
