/**
 * verinice.veo accounts
 * Copyright (C) 2025  Jonas Jordan
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

class ClientConfigRestTest : AbstractRestTest() {
    lateinit var clientManagerId: String

    @BeforeEach
    fun setup() {
        clientManagerId = createManager(createVeoClientGroup())
    }

    @Test
    fun `client config can be managed`() {
        // when fetching default config
        var clientConfig =
            get(
                "/client-config",
                clientManagerId,
            ).bodyAsMap

        // then unit access is not restricted
        clientConfig shouldBe
            mapOf(
                "restrictUnitAccess" to false,
            )

        // when restricting unit access
        clientConfig["restrictUnitAccess"] = true
        put(
            "/client-config",
            clientManagerId,
            clientConfig,
        )
        clientConfig = get("/client-config", clientManagerId).bodyAsMap

        // then the change has been applied
        clientConfig["restrictUnitAccess"] shouldBe true

        // when opening unit access
        clientConfig["restrictUnitAccess"] = false
        put(
            "/client-config",
            clientManagerId,
            clientConfig,
        )
        clientConfig = get("/client-config", clientManagerId).bodyAsMap

        // then the change has been applied
        clientConfig["restrictUnitAccess"] shouldBe false
    }
}
