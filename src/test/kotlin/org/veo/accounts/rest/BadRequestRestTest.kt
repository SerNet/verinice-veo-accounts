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

class BadRequestRestTest : AbstractRestTest() {
    lateinit var managerId: String

    @BeforeEach
    fun setup() {
        managerId = createAccount(createVeoClientGroup())
    }

    @Test
    fun `cannot create account without username`() {
        post(
            "/",
            managerId,
            mapOf(
                "emailAddress" to "$prefix-hansi@test.test",
                "firstName" to "Hansi",
                "lastName" to "Dance",
                "groups" to listOf("veo-write-access"),
                "enabled" to false
            ),
            400
        ).rawBody shouldBe "username must not be null"
    }

    @Test
    fun `cannot create account with invalid group`() {
        post(
            "/",
            managerId,
            mapOf(
                "username" to "hansi",
                "emailAddress" to "$prefix-hansi@test.test",
                "firstName" to "Hansi",
                "lastName" to "Dance",
                "groups" to listOf("veo-night-access"),
                "enabled" to false
            ),
            400
        ).rawBody shouldBe "Cannot deserialize value of type `org.veo.accounts.AssignableGroup` from String \"veo-night-access\": not one of the values accepted for Enum class: [veo-write-access]"
    }

    @Test
    fun `cannot create account with empty username`() {
        post(
            "/",
            managerId,
            mapOf(
                "username" to "",
                "emailAddress" to "$prefix-hansi@test.test",
                "firstName" to "Hansi",
                "lastName" to "Dance",
                "groups" to listOf("veo-write-access"),
                "enabled" to false
            ),
            400
        ).rawBody shouldBe "Invalid Username: size must be between 1 and 256"
    }
}
