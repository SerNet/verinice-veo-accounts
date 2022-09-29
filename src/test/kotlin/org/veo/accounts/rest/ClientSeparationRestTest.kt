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

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClientSeparationRestTest : AbstractRestTest() {
    lateinit var client1ManagerId: String
    lateinit var client2ManagerId: String

    @BeforeEach
    fun setup() {
        client1ManagerId = createAccount(createVeoClientGroup())
        client2ManagerId = createAccount(createVeoClientGroup())
    }

    @Test
    fun `created account cannot be accessed by another client`() {
        // when an account is created in client 1
        val accountId = post(
            "/",
            client1ManagerId,
            mapOf(
                "username" to "$prefix-louis",
                "firstName" to "Louis",
                "lastName" to "King",
                "emailAddress" to "$prefix-louis@client1.test",
                "groups" to emptyList<String>(),
                "enabled" to true
            )
        ).rawBody

        // then client 1 manager can access it
        get("/$accountId", client1ManagerId).bodyAsMap["username"] shouldBe "$prefix-louis"

        // and client 2 manager cannot access it
        get("/$accountId", client2ManagerId, 404)

        // and client 2 manager cannot see it in the list
        get("/", client2ManagerId).bodyAsListOfMaps shouldHaveSize 0

        // and client 2 cannot update it
        put(
            "/$accountId",
            client2ManagerId,
            mapOf(
                "emailAddress" to "$prefix-evil@string.test",
                "firstName" to "Louis",
                "lastName" to "King",
                "groups" to emptyList<String>(),
                "enabled" to true
            ),
            404
        )
        get("/$accountId", client1ManagerId).bodyAsMap["emailAddress"] shouldBe "$prefix-louis@client1.test"

        // and client 2 cannot delete it
        delete("/$accountId", client2ManagerId, 404)
        get("/$accountId", client1ManagerId)
    }
}
