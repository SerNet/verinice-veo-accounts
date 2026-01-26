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

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.veo.accounts.dtos.VeoClientId

class MaxUsersRestTest : AbstractRestTest() {
    lateinit var client: VeoClientId
    lateinit var managerId: String

    @BeforeEach
    fun setup() {
        client = createVeoClientGroup(3)
        managerId = createManager(client)
    }

    @Test
    fun `cannot exceed maxUsers`() {
        // Given an account with a maximum of 3 accounts, a manager and two more accounts
        val account2Id = postAccount("two", true).bodyAsMap["id"]
        val account3Id = postAccount("three", true).bodyAsMap["id"]

        // expect that an additional enabled account cannot be created
        postAccount("four", true, 403)
            .rawBody shouldBe "Your veo license only allows up to 3 enabled account(s)"

        // when disabling an existing account
        get("/$account3Id", managerId).bodyAsMap.let {
            it["enabled"] = false
            put("/$account3Id", managerId, it)
        }

        // then an additional enabled account can be created
        postAccount("four", true)

        // expect that re-enabling the disabled account fails
        get("/$account3Id", managerId).bodyAsMap.let {
            it["enabled"] = true
            put("/$account3Id", managerId, it, 403)
                .rawBody shouldBe "Your veo license only allows up to 3 enabled account(s)"
        }

        // when deleting an active account
        delete("/$account2Id", managerId)

        // then the disabled account can be re-enabled
        get("/$account3Id", managerId).bodyAsMap.let {
            it["enabled"] = true
            put("/$account3Id", managerId, it)
        }

        // expect that a disabled account can always be created
        postAccount("user4", false)
    }

    @Test
    fun `account managers count as users`() {
        // when maxUsers is exhausted with the existing manager, another manager and a third user
        createManager(client)
        postAccount("three", true)

        // then no more enabled accounts can be created
        postAccount("four", true, 403)
            .rawBody shouldBe "Your veo license only allows up to 3 enabled account(s)"
    }

    @Test
    fun `shrinking maxUsers is handled`(): Unit =
        runBlocking(IO) {
            // Given a manager, two more enabled accounts and one disabled account
            val account2Id = postAccount("two", true).bodyAsMap["id"]
            val account3Id = postAccount("three", true).bodyAsMap["id"]
            val account4Id = postAccount("four", false).bodyAsMap["id"]

            // when reducing the maximum amount of accounts to 2
            updateMaxUsers(client, 2)

            // then an existing account can still be modified
            get("/$account2Id", managerId).bodyAsMap.let {
                it["firstName"] = "Neo"
                put("/$account2Id", managerId, it)
            }

            // and enabling a disabled account is forbidden
            get("/$account4Id", managerId).bodyAsMap.let {
                it["enabled"] = true
                put("/$account4Id", managerId, it, 403)
                    .rawBody shouldBe "Your veo license only allows up to 2 enabled account(s)"
            }

            // and adding another enabled account is forbidden
            postAccount("five", true, 403)
                .rawBody shouldBe "Your veo license only allows up to 2 enabled account(s)"

            // when deleting two enabled accounts
            delete("/$account2Id", managerId)
            delete("/$account3Id", managerId)

            // then a disabled account can be enabled
            get("/$account4Id", managerId).bodyAsMap.let {
                it["enabled"] = true
                put("/$account4Id", managerId, it)
            }

            // when deleting one enabled account
            delete("/$account4Id", managerId)

            // then a new enabled account can be created
            postAccount("five", true)
        }

    /** The timing in this test is unpredictable. It may produce false successful results. */
    @Test
    fun `maxUsers cannot be exceeded using simultaneous POSTs`() =
        runBlocking(IO) {
            // When trying to create many accounts at once and waiting for all operations to finish
            var failedAttempts = 0
            (1..20)
                .map { i ->
                    async {
                        when (postAccount("user$i", true, null).statusCode) {
                            403 -> failedAttempts++
                            201 -> Unit
                            else -> throw IllegalStateException()
                        }
                    }
                }.forEach { it.await() }

            // then the maximum amount of enabled accounts has not been exceeded
            failedAttempts shouldBe 18
            get("/", managerId).bodyAsListOfMaps.size shouldBe 2
        }

    /** The timing in this test is unpredictable. It may produce false successful results. */
    @Test
    fun `maxUsers cannot be exceeded using simultaneous PUTs`() =
        runBlocking(IO) {
            // Given many disabled accounts
            val accounts =
                (1..20)
                    .map { postAccount("user$it", false).bodyAsMap["id"] }
                    .map { get("/$it", managerId).bodyAsMap }

            // when trying to enable all accounts at once and waiting for all operations to finish
            var failedAttempts = 0
            accounts
                .map {
                    async {
                        it["enabled"] = true
                        when (put("/${it["id"]}", managerId, it, null).statusCode) {
                            403 -> failedAttempts++
                            204 -> Unit
                            else -> throw IllegalStateException()
                        }
                    }
                }.forEach { it.await() }

            // then the maximum amount of enabled accounts has not been exceeded
            failedAttempts shouldBe 18
            get("/", managerId)
                .bodyAsListOfMaps
                .filter { it["enabled"] == true }
                .size shouldBe 2
        }

    private fun postAccount(
        username: String,
        enabled: Boolean,
        expectedStatus: Int? = 201,
    ) = post(
        "/",
        managerId,
        mapOf(
            "username" to "$prefix-$username",
            "emailAddress" to "$prefix-$username@test.test",
            "firstName" to "Max",
            "lastName" to "Users",
            "groups" to listOf("veo-write-access"),
            "enabled" to enabled,
        ),
        expectedStatus,
    )
}
