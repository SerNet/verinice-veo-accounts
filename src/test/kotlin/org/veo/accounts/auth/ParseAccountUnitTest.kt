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
package org.veo.accounts.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.veo.accounts.dtos.AccountId
import java.util.UUID

class ParseAccountUnitTest {
    @Test
    fun `parses valid token`() {
        val auth = mockk<JwtAuthenticationToken> {
            every { name } returns "32beff50-f073-495f-a86f-ebd1fc218551"
            every { token } returns mockk {
                every { getClaimAsStringList("groups") } returns listOf(
                    "veo-user",
                    "/veo_client:5201f42d-eee8-4d4e-a18f-2504cc0ce11e",
                    "some-random-other-group",
                )
            }
        }

        auth.parseAccount().apply {
            id shouldBe AccountId("32beff50-f073-495f-a86f-ebd1fc218551")
            veoClient.apply {
                groupName shouldBe "veo_client:5201f42d-eee8-4d4e-a18f-2504cc0ce11e"
                clientId shouldBe UUID.fromString("5201f42d-eee8-4d4e-a18f-2504cc0ce11e")
            }
        }
    }

    @Test
    fun `does not accept token with multiple client groups`() {
        val auth = mockk<JwtAuthenticationToken> {
            every { name } returns "32beff50-f073-495f-a86f-ebd1fc218551"
            every { token } returns mockk {
                every { getClaimAsStringList("groups") } returns listOf(
                    "veo-user",
                    "/veo_client:5201f42d-eee8-4d4e-a18f-2504cc0ce11e",
                    "/veo_client:d7c687bc-46a5-4bfa-a29e-b63ca6a3ec54",
                    "some-random-other-group",
                )
            }
        }

        shouldThrow<IllegalArgumentException> { auth.parseAccount() }
    }

    @Test
    fun `does not accept token without client groups`() {
        val auth = mockk<JwtAuthenticationToken> {
            every { name } returns "32beff50-f073-495f-a86f-ebd1fc218551"
            every { token } returns mockk {
                every { getClaimAsStringList("groups") } returns listOf(
                    "veo-user",
                    "some-random-other-group",
                )
            }
        }

        shouldThrow<IllegalArgumentException> { auth.parseAccount() }
    }
}
