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

class UniquenessRestTest : AbstractRestTest() {
    lateinit var managerId: String

    @BeforeEach
    fun setup() {
        managerId = createAccount(createVeoClientGroup())
    }

    @Test
    fun `usernames must be unique`() {
        // given an existing user
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-snowflake",
                "firstName" to "Snowy",
                "lastName" to "Flaky",
                "emailAddress" to "$prefix-special@snowflake.test",
                "groups" to emptyList<String>()
            )
        )

        // expect that creating another account with the same username will fail
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-snowflake",
                "firstName" to "Copy",
                "lastName" to "Kitty",
                "emailAddress" to "$prefix-copy@cat.test",
                "groups" to emptyList<String>()
            ),
            409
        ).rawBody shouldBe "Username or email address already taken"
    }

    @Test
    fun `email address for new account must be unique`() {
        // given an existing account
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-snowflake",
                "emailAddress" to "$prefix-special@snowflake.test",
                "firstName" to "Snowy",
                "lastName" to "Flaky",
                "groups" to emptyList<String>()
            )
        )

        // expect that creating another account with the same email address will fail
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-copycat",
                "firstName" to "Copy",
                "lastName" to "Kitty",
                "emailAddress" to "$prefix-special@snowflake.test",
                "groups" to emptyList<String>()
            ),
            409
        ).rawBody shouldBe "Username or email address already taken"
    }

    @Test
    fun `new email address for existing account must be unique`() {
        // given two accounts
        post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-snowflake",
                "firstName" to "Snowy",
                "lastName" to "Flaky",
                "emailAddress" to "$prefix-special@snowflake.test",
                "groups" to emptyList<String>()
            )
        )
        val secondAccountId = post(
            "/",
            managerId,
            mapOf(
                "username" to "$prefix-glowcake",
                "firstName" to "Glowy",
                "lastName" to "Cakey",
                "emailAddress" to "$prefix-special@glowcake.test",
                "groups" to emptyList<String>()
            )
        ).rawBody!!

        // expect that updating the second account with the first account's email address will fail
        put(
            "/$secondAccountId",
            managerId,
            mapOf(
                "emailAddress" to "$prefix-special@snowflake.test",
                "firstName" to "Glowy",
                "lastName" to "Cakey",
                "groups" to emptyList<String>()
            ),
            409
        ).rawBody shouldBe "Email address already taken"
    }
}
