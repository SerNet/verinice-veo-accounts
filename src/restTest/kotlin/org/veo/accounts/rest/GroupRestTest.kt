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

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.veo.accounts.asMap
import java.util.UUID.randomUUID
import kotlin.collections.emptyList

class GroupRestTest : AbstractRestTest() {
    lateinit var managerId: String

    @BeforeEach
    fun setup() {
        managerId = createManager(createVeoClientGroup())
    }

    @Test
    fun `groups can be managed`() {
        // when an account is created without groups
        val accountId =
            post(
                "/",
                managerId,
                mapOf(
                    "username" to "$prefix-hans",
                    "firstName" to "Hans",
                    "lastName" to "Dance",
                    "emailAddress" to "$prefix-hans@test.test",
                    "groups" to emptyList<String>(),
                    "enabled" to true,
                ),
            ).bodyAsMap["id"]
        var accountBody = get("/$accountId", managerId).bodyAsMap

        // then it was saved only with the client group
        accountBody["groups"] shouldBe emptyList<String>()

        // when updating account with a group and re-fetching
        accountBody["groups"] = listOf("veo-write-access")
        put("/$accountId", managerId, accountBody)
        accountBody = get("/$accountId", managerId).bodyAsMap

        // then the group was assigned
        accountBody["groups"] shouldBe listOf("veo-write-access")

        // when updating account without the group and re-fetching
        accountBody["groups"] = emptyList<String>()
        put("/$accountId", managerId, accountBody)
        accountBody = get("/$accountId", managerId).bodyAsMap

        // then the group was removed
        accountBody["groups"] shouldBe emptyList<String>()
    }

    @Test
    fun `access groups can be managed`() {
        // given
        val unitId1 = randomUUID().toString()
        val unitId2 = randomUUID().toString()

        // when creating an access group
        val accessGroup1Id =
            post(
                "/access-groups",
                managerId,
                mapOf(
                    "name" to "Early access",
                    "units" to
                        mapOf(
                            unitId1 to "READ_ONLY",
                            unitId2 to "READ_WRITE",
                        ),
                ),
            ).bodyAsMap["id"]

        // then it can be retrieved
        get("/access-groups/$accessGroup1Id", managerId).bodyAsMap.let {
            it["id"] shouldBe accessGroup1Id
            it["name"] shouldBe "Early access"
            it["units"] shouldBe
                mapOf(
                    unitId1 to "READ_ONLY",
                    unitId2 to "READ_WRITE",
                )
            (it["_self"] as String) shouldMatch "https?://.*/access-groups/$accessGroup1Id"
        }

        // when updating the group
        get("/access-groups/$accessGroup1Id", managerId).bodyAsMap.let {
            it["name"] = "Earlier access"
            it["units"].asMap().put(unitId1, "READ_WRITE")
            put(it["_self"] as String, managerId, it)
        }

        // then the changes have been applied
        get("/access-groups/$accessGroup1Id", managerId).bodyAsMap.let {
            it["name"] shouldBe "Earlier access"
            it["units"].asMap().get(unitId1) shouldBe "READ_WRITE"
        }

        // when creating another access group
        post(
            "/access-groups",
            managerId,
            mapOf(
                "name" to "Supergroup",
                "units" to
                    mapOf(
                        unitId2 to "READ_ONLY",
                    ),
            ),
        )
        // both can be retrieved
        get("/access-groups", managerId).bodyAsListOfMaps.map { it["name"] } shouldContainExactlyInAnyOrder
            listOf("Earlier access", "Supergroup")

        // when deleting an access group
        delete("/access-groups/$accessGroup1Id", managerId)

        // then it is gone
        get("/access-groups/$accessGroup1Id", managerId, 404).rawBody shouldBe "Access group $accessGroup1Id not found"
    }

    @Test
    fun `account can be created with groups`() {
        // given some access groups
        val accessGroup1Id = createAccessGroup(managerId)
        val accessGroup2Id = createAccessGroup(managerId)
        val accessGroup3Id = createAccessGroup(managerId)

        // when an account is created with groups
        val accountId =
            post(
                "/",
                managerId,
                mapOf(
                    "username" to "$prefix-hans",
                    "firstName" to "Hans",
                    "lastName" to "Dance",
                    "emailAddress" to "$prefix-hans@test.test",
                    "groups" to listOf("veo-write-access"),
                    "accessGroups" to listOf(accessGroup1Id, accessGroup2Id),
                    "enabled" to true,
                ),
            ).bodyAsMap["id"]

        // then it is assigned to groups
        get("/$accountId", managerId).bodyAsMap.let {
            it["groups"] shouldBe listOf("veo-write-access")
            (it["accessGroups"] as List<*>) shouldContainExactlyInAnyOrder listOf(accessGroup1Id, accessGroup2Id)
        }

        // when the account's memberships are changed
        get("/$accountId", managerId).bodyAsMap.also {
            it["groups"] = emptyList<String>()
            it["accessGroups"] = listOf(accessGroup1Id, accessGroup3Id)
            put("/$accountId", managerId, it)
        }

        // then it is assigned to other groups
        get("/$accountId", managerId).bodyAsMap.let {
            it["groups"] shouldBe emptyList<String>()
            (it["accessGroups"] as List<*>) shouldContainExactlyInAnyOrder listOf(accessGroup1Id, accessGroup3Id)
        }

        // when deleting an access group
        delete("/access-groups/$accessGroup1Id", managerId)

        // then it is removed from the account
        get("/$accountId", managerId).bodyAsMap.let {
            it["accessGroups"] shouldBe listOf(accessGroup3Id)
        }
    }

    @Test
    fun `account cannot be created in forbidden group`() {
        // expect that admin group assignment should fail
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-hans",
                "firstName" to "Hans",
                "lastName" to "Dance",
                "emailAddress" to "$prefix-hans@test.test",
                "groups" to listOf("veo-admin"),
                "enabled" to true,
            ),
            400,
        )
    }

    @Test
    fun `account cannot join forbidden group`() {
        // given an account with no groups
        val accountId =
            post(
                "/",
                managerId,
                mapOf(
                    "username" to "$prefix-hans",
                    "firstName" to "Hans",
                    "lastName" to "Dance",
                    "emailAddress" to "$prefix-hans@test.test",
                    "groups" to emptyList<String>(),
                    "enabled" to true,
                ),
            ).bodyAsMap["id"]

        // expect that adding it to admin group should fail
        put(
            "/$accountId",
            managerId,
            mapOf(
                "firstName" to "Hans",
                "lastName" to "Dance",
                "emailAddress" to "$prefix-hans@test.test",
                "groups" to listOf("veo-admin"),
                "enabled" to true,
            ),
            400,
        )
    }

    @Test
    fun `account cannot join absent access group`() {
        // given an absent access group id
        val randomAccessGroupId = randomUUID()

        // expect a group assignment to fail
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-lance",
                "firstName" to "Lance",
                "lastName" to "Stance",
                "emailAddress" to "$prefix-lance@test.test",
                "groups" to emptyList<String>(),
                "accessGroups" to listOf(randomAccessGroupId),
                "enabled" to true,
            ),
            422,
        ).rawBody shouldBe "Access group $randomAccessGroupId not found"
    }
}
