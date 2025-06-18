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
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import org.veo.accounts.Role.CREATE
import org.veo.accounts.Role.DELETE
import org.veo.accounts.Role.READ
import org.veo.accounts.Role.UPDATE
import org.veo.accounts.asListOfMaps
import org.veo.accounts.asMap
import java.util.UUID.randomUUID

class SecurityRestTest : AbstractRestTest() {
    private val randomUuid = randomUUID()

    @Test
    fun `all API calls are forbidden without authorization`() {
        get("/", null, 401)
        get("/$randomUuid", null, 401)
        post("/", null, null, 401)
        put("/$randomUuid", null, null, 401)
        delete("/$randomUuid", null, 401)
    }

    @Test
    fun `CRUD managers have full access`() {
        val managerId = createManager(createVeoClientGroup())

        get("/", managerId, 200).rawBody shouldBe "[]"
        get("/$randomUuid", managerId, 404).rawBody shouldBe "Resource not found"
        post("/", managerId, expectedStatus = 400).rawBody shouldMatch Regex("Required request body is missing.*")
        put("/$randomUuid", managerId, expectedStatus = 400).rawBody shouldMatch Regex("Required request body is missing.*")
        delete("/$randomUuid", managerId, 404).rawBody shouldBe "Resource not found"
    }

    @Test
    fun `readers have limited access`() {
        val readerId = createManager(createVeoClientGroup(), roles = listOf(READ))

        get("/", readerId, 200).rawBody shouldBe "[]"
        get("/$randomUuid", readerId, 404).rawBody shouldBe "Resource not found"

        post("/", readerId, null, 403)
        put("/$randomUuid", readerId, null, 403)
        delete("/$randomUuid", readerId, 403)
    }

    @Test
    fun `creators have limited access`() {
        val creatorId = createManager(createVeoClientGroup(), roles = listOf(CREATE))

        post("/", creatorId, expectedStatus = 400).rawBody shouldMatch Regex("Required request body is missing.*")

        get("/", creatorId, 403)
        get("/$randomUuid", creatorId, 403)
        put("/$randomUuid", creatorId, null, 403)
        delete("/$randomUuid", creatorId, 403)
    }

    @Test
    fun `updaters have limited access`() {
        val updaterId = createManager(createVeoClientGroup(), roles = listOf(UPDATE))

        put("/$randomUuid", updaterId, expectedStatus = 400).rawBody shouldMatch Regex("Required request body is missing.*")

        get("/", updaterId, 403)
        get("/$randomUuid", updaterId, 403)
        post("/", updaterId, null, 403)
        delete("/$randomUuid", updaterId, 403)
    }

    @Test
    fun `deleters have limited access`() {
        val deleterId = createManager(createVeoClientGroup(), roles = listOf(DELETE))

        delete("/$randomUuid", deleterId, 404).rawBody shouldBe "Resource not found"

        get("/", deleterId, 403)
        get("/$randomUuid", deleterId, 403)
        post("/", deleterId, null, 403)
        put("/$randomUuid", deleterId, null, 403)
    }

    @Test
    fun `API key works for initial account creation`() {
        post("/initial", headers = mapOf("Authorization" to listOf(clientInitApiKey)), expectedStatus = 400).rawBody shouldMatch
            Regex("Required request body is missing.*")

        post("/initial", headers = mapOf("Authorization" to listOf("wrongKey")), expectedStatus = 401)
        post("/initial", expectedStatus = 401)
    }

    @Test
    fun `API key does not work for account management`() {
        val headers = mapOf("Authorization" to listOf(clientInitApiKey))

        get("/", headers = headers, expectedStatus = 401)
        get("/$randomUuid", headers = headers, expectedStatus = 401)
        post("/", headers = headers, expectedStatus = 401)
        put("/$randomUuid", null, null, headers = headers, expectedStatus = 401)
        delete("/$randomUuid", headers = headers, expectedStatus = 401)
    }

    @Test
    fun `monitoring & documentation are accessible`() {
        get("/actuator/health/readiness", expectedStatus = 200)
        get("/actuator/health/liveness", expectedStatus = 200)

        get("/swagger-ui.html", expectedStatus = 200)
        get("/swagger-ui/index.html", expectedStatus = 200)
        get("/v3/api-docs", expectedStatus = 200)
        get("/v3/api-docs/swagger-config", expectedStatus = 200)
    }

    @Test
    fun `authorization methods are documented`() {
        val docs = get("/v3/api-docs").bodyAsMap

        docs["components"].asMap()["securitySchemes"].asMap().let {
            it["OAuth2"].asMap()["type"] shouldBe "oauth2"
            it["ClientInitApiKey"].asMap()["type"] shouldBe "apiKey"
        }

        val endpointDocs =
            docs["paths"].asMap().flatMap { pathEntry ->
                pathEntry.value.asMap().map { endpointEntry ->
                    EndpointDoc(
                        pathEntry.key,
                        endpointEntry.key,
                        endpointEntry.value
                            .asMap()["security"]
                            .asListOfMaps()
                            .flatMap { it.keys },
                    )
                }
            }

        endpointDocs.forEach {
            if (it.path == "/initial" && it.httpMethod == "post") {
                it.securitySchemes shouldBe listOf("ClientInitApiKey")
            } else {
                it.securitySchemes shouldBe listOf("OAuth2")
            }
        }
    }

    data class EndpointDoc(
        val path: String,
        val httpMethod: String,
        val securitySchemes: List<String>,
    )
}
