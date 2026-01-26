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
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

class AccountManagementRestTest : AbstractRestTest() {
    lateinit var managerId: String

    @BeforeEach
    fun setup() {
        managerId = createManager(createVeoClientGroup())
    }

    @Test
    fun `CRUD an account`() {
        // expect that an account can be created
        val accountId =
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
            ).bodyAsMap["id"]
        accountId shouldNotBe null

        // and retrieved
        get("/$accountId", managerId).bodyAsMap.apply {
            get("username") shouldBe "$prefix-hans"
            get("emailAddress") shouldBe "$prefix-hans@test.test"
            get("firstName") shouldBe "Hans"
            get("lastName") shouldBe "Dance"
            get("groups") shouldBe listOf("veo-write-access")
            get("enabled") shouldBe true
            get("language") shouldBe "de"
        }

        // and found in the list
        get("/", managerId)
            .bodyAsListOfMaps
            .first { it["id"] == accountId }
            .also { hans ->
                hans["username"] shouldBe "$prefix-hans"
                hans["emailAddress"] shouldBe "$prefix-hans@test.test"
                hans["firstName"] shouldBe "Hans"
                hans["lastName"] shouldBe "Dance"
                hans["groups"] shouldBe listOf("veo-write-access")
                hans["enabled"] shouldBe true
                hans["language"] shouldBe "de"
                (hans["_self"] as String).let {
                    it shouldMatch "https?://.*/$accountId"
                    get(it, managerId, 200)
                }
            }

        // and updated
        put(
            "/$accountId",
            managerId,
            mapOf(
                "emailAddress" to "$prefix-hansi@test.test",
                "firstName" to "Hansi",
                "lastName" to "Dance",
                "groups" to listOf("veo-write-access"),
                "enabled" to false,
                "language" to "fr",
            ),
        )
        get("/$accountId", managerId).bodyAsMap.apply {
            get("username") shouldBe "$prefix-hans"
            get("firstName") shouldBe "Hansi"
            get("lastName") shouldBe "Dance"
            get("emailAddress") shouldBe "$prefix-hansi@test.test"
            get("groups") shouldBe listOf("veo-write-access")
            get("enabled") shouldBe false
            get("language") shouldBe "fr"
        }

        // and deleted
        delete("/$accountId", managerId)
        get("/$accountId", managerId, 404)
    }

    @Test
    fun `create multiple accounts`() {
        // expect that the manager is not listed
        get("/", managerId).bodyAsListOfMaps shouldHaveSize 0

        // when creating two accounts
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-paula",
                "emailAddress" to "$prefix-pla@test.test",
                "firstName" to "Paula",
                "lastName" to "Fowler",
                "emailAddress" to "$prefix-pla@test.test",
                "groups" to emptyList<String>(),
                "enabled" to true,
            ),
        )
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-hubert",
                "emailAddress" to "$prefix-hub@test.test",
                "firstName" to "Hubert",
                "lastName" to "Hugh-Bird",
                "emailAddress" to "$prefix-hub@test.test",
                "groups" to emptyList<String>(),
                "enabled" to true,
            ),
        )

        // then they appear in the list
        get("/", managerId).bodyAsListOfMaps.apply {
            size shouldBe 2
            map { it["username"] } shouldContainExactlyInAnyOrder listOf("$prefix-paula", "$prefix-hubert")
        }
    }

    @Test
    fun `account manager cannot manage themselves`() {
        // expect that authenticated account is not listed
        get("/", managerId).bodyAsListOfMaps shouldHaveSize 0

        // and it cannot be retrieved individually
        get("/$managerId", managerId, 404)

        // and it cannot be updated
        put(
            "/$managerId",
            managerId,
            mapOf(
                "emailAddress" to "$prefix-manager@test.test",
                "firstName" to "Man",
                "lastName" to "Ager",
                "groups" to emptyList<String>(),
                "enabled" to true,
            ),
            404,
        )

        // and it cannot be deleted
        delete("/$managerId", managerId, 404)

        // and it still works after deletion attempt
        get("/", managerId)
    }

    @Test
    fun `usernames cannot be updated`() {
        // given an existing user
        val accountId =
            post(
                "/",
                managerId,
                mapOf(
                    "username" to "$prefix-power-user",
                    "firstName" to "Tara",
                    "lastName" to "Rattan",
                    "emailAddress" to "$prefix-user@power.test",
                    "groups" to emptyList<String>(),
                    "enabled" to true,
                ),
            ).bodyAsMap["id"]

        // when trying to change the username
        put(
            "/$accountId",
            managerId,
            mapOf(
                "username" to "$prefix-power-loser",
                "emailAddress" to "$prefix-user@power.test",
                "firstName" to "Tara",
                "lastName" to "Rattan",
                "emailAddress" to "$prefix-user@power.test",
                "groups" to emptyList<String>(),
                "enabled" to true,
            ),
        )

        // then nothing changed
        get("/$accountId", managerId).bodyAsMap["username"] shouldBe "$prefix-power-user"
    }

    @Test
    fun `operations on absent account produce 404s`() {
        // given a random account ID
        val randId = randomUUID()

        // expect all operations on the ID to produce 404s
        get("/$randId", managerId, 404)
        put(
            "/$randId",
            managerId,
            mapOf(
                "emailAddress" to "$prefix-randy@test.test",
                "firstName" to "Randy",
                "lastName" to "Random",
                "groups" to emptyList<String>(),
                "enabled" to true,
            ),
            404,
        )
        delete("/$randId", managerId, 404)
    }
}
