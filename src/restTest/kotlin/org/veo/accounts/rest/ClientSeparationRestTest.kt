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
        client1ManagerId = createManager(createVeoClientGroup())
        client2ManagerId = createManager(createVeoClientGroup())
    }

    @Test
    fun `created account cannot be accessed by another client`() {
        // when an account is created in client 1
        val accountId =
            post(
                "/",
                client1ManagerId,
                mapOf(
                    "username" to "$prefix-louis",
                    "firstName" to "Louis",
                    "lastName" to "King",
                    "emailAddress" to "$prefix-louis@client1.test",
                    "groups" to emptyList<String>(),
                    "enabled" to true,
                ),
            ).bodyAsMap["id"]

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
                "enabled" to true,
            ),
            404,
        )
        get("/$accountId", client1ManagerId).bodyAsMap["emailAddress"] shouldBe "$prefix-louis@client1.test"

        // and client 2 cannot delete it
        delete("/$accountId", client2ManagerId, 404)
        get("/$accountId", client1ManagerId)
    }

    @Test
    fun `created access group cannot be accessed by another client`() {
        // when an access group is created in client 1
        val accessGroupId =
            post(
                "/access-groups",
                client1ManagerId,
                mapOf(
                    "name" to "Private group",
                ),
            ).bodyAsMap["id"]

        // then client 1 manager can access it
        get("/access-groups/$accessGroupId", client1ManagerId).bodyAsMap["name"] shouldBe "Private group"

        // and client 2 manager cannot access it
        get("/access-groups/$accessGroupId", client2ManagerId, 404).rawBody shouldBe "Access group $accessGroupId not found"

        // and client 2 manager cannot see it in the list
        get("/access-groups", client2ManagerId).bodyAsListOfMaps shouldHaveSize 0

        // and client 2 cannot update it
        put(
            "/access-groups/$accessGroupId",
            client2ManagerId,
            mapOf(
                "name" to "Kidnapped group",
            ),
            404,
        ).rawBody shouldBe "Access group $accessGroupId not found"

        // and client 2 cannot delete it
        delete("/access-groups/$accessGroupId", client2ManagerId, 404).rawBody shouldBe "Access group $accessGroupId not found"

        // and it remains untouched
        get("/access-groups/$accessGroupId", client1ManagerId).bodyAsMap["name"] == "Private group"
    }

    @Test
    fun `users cannot join access groups from other clients`() {
        // given an access group in client 1
        val accessGroupId =
            post(
                "/access-groups",
                client1ManagerId,
                mapOf(
                    "name" to "Secret group",
                ),
            ).bodyAsMap["id"]

        // expect that a new user in the same client can join it
        post(
            "/",
            client1ManagerId,
            mapOf(
                "username" to "good-guy",
                "emailAddress" to "$prefix-good-guy@example",
                "firstName" to "Guy",
                "lastName" to "Good",
                "groups" to emptyList<String>(),
                "accessGroups" to listOf(accessGroupId),
                "enabled" to true,
            ),
        )

        // but a new user in client 2 cannot join
        post(
            "/",
            client2ManagerId,
            mapOf(
                "username" to "bad-gal",
                "emailAddress" to "$prefix-bad-gal@example",
                "firstName" to "Gabriella",
                "lastName" to "Baden",
                "groups" to emptyList<String>(),
                "accessGroups" to listOf(accessGroupId),
                "enabled" to true,
            ),
            422,
        ).rawBody shouldBe "Access group $accessGroupId not found"

        // and an existing user in client 2 cannot join, either
        val client2AccountId =
            post(
                "/",
                client2ManagerId,
                mapOf(
                    "username" to "new-guy",
                    "emailAddress" to "$prefix-new-guy@example",
                    "firstName" to "Guy",
                    "lastName" to "Newman",
                    "groups" to emptyList<String>(),
                    "enabled" to true,
                ),
            ).bodyAsMap["id"]
        get("/$client2AccountId", client2ManagerId).bodyAsMap.let {
            it["accessGroups"] = listOf(accessGroupId)
            put("/$client2AccountId", client2ManagerId, it, 422).rawBody shouldBe "Access group $accessGroupId not found"
        }
    }

    @Test
    fun `client configs are isolated`() {
        // when client 1 has restricted unit access
        put(
            "/client-config",
            client1ManagerId,
            mapOf("restrictUnitAccess" to true),
        )

        // then this applies to client 1
        get("/client-config", client1ManagerId).bodyAsMap["restrictUnitAccess"] shouldBe true

        // and not to client 2
        get("/client-config", client2ManagerId).bodyAsMap["restrictUnitAccess"] shouldBe false
    }
}
