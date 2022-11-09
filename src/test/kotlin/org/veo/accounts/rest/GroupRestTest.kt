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

class GroupRestTest : AbstractRestTest() {
    lateinit var managerId: String

    @BeforeEach
    fun setup() {
        managerId = createManager(createVeoClientGroup())
    }

    @Test
    fun `groups can be managed`() {
        // when an account is created without groups
        val accountId = post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-hans",
                "firstName" to "Hans",
                "lastName" to "Dance",
                "emailAddress" to "$prefix-hans@test.test",
                "groups" to emptyList<String>(),
                "enabled" to true
            )
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
    fun `account can be created with groups`() {
        // when an account is created with group
        val accountId = post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-hans",
                "firstName" to "Hans",
                "lastName" to "Dance",
                "emailAddress" to "$prefix-hans@test.test",
                "groups" to listOf("veo-write-access"),
                "enabled" to true
            )
        ).bodyAsMap["id"]

        // then it is assigned to group
        get("/$accountId", managerId).bodyAsMap.apply {
            get("groups") shouldBe listOf("veo-write-access")
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
                "enabled" to true
            ),
            400
        )
    }

    @Test
    fun `account cannot join forbidden group`() {
        // given an account with no groups
        val accountId = post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-hans",
                "firstName" to "Hans",
                "lastName" to "Dance",
                "emailAddress" to "$prefix-hans@test.test",
                "groups" to emptyList<String>(),
                "enabled" to true
            )
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
                "enabled" to true
            ),
            400
        )
    }
}
