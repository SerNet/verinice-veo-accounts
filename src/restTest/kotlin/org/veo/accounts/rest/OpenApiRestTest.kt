/*
 * verinice.veo accounts
 * Copyright (C) 2026  Jonas Jordan
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

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OpenApiRestTest : AbstractRestTest() {
    @Test
    fun `responses are documented correctly`() =
        assertSoftly {
            parseEndpointDocs(get("/v3/api-docs")).forEach {
                val endpoint = it.toString()
                withClue(endpoint) {
                    it.responseCodes shouldBe
                        when (endpoint) {
                            "POST /clients" -> setOf("201")
                            "PUT /clients/{clientId}" -> setOf("204")
                            "PUT /cliients/{clientId}" -> setOf("204")
                            "DELETE /clients/{clientId}" -> setOf("204")
                            "POST /clients/{clientId}/activation" -> setOf("204")
                            "POST /clients/{clientId}/deactivation" -> setOf("204")
                            "POST /initial" -> setOf("201", "409", "422")
                            "GET /" -> setOf("200")
                            "POST /" -> setOf("201")
                            "GET /{id}" -> setOf("200")
                            "PUT /{id}" -> setOf("204")
                            "DELETE /{id}" -> setOf("204")
                            "GET /client-config" -> setOf("200")
                            "PUT /client-config" -> setOf("204")
                            "GET /access-groups" -> setOf("200")
                            "POST /access-groups" -> setOf("201")
                            "GET /access-groups/{id}" -> setOf("200")
                            "PUT /access-groups/{id}" -> setOf("204")
                            "DELETE /access-groups/{id}" -> setOf("204")
                            "PUT /admin/license" -> setOf("204")
                            else -> null // fail softly
                        }
                }
            }
        }
}
