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

import org.junit.jupiter.api.Test
import org.veo.accounts.Role.CREATE
import org.veo.accounts.Role.DELETE
import org.veo.accounts.Role.READ
import org.veo.accounts.Role.UPDATE
import java.util.UUID.randomUUID

class SecurityRestTest : AbstractRestTest() {
    @Test
    fun `all API calls are forbidden without authorization`() {
        get("/", null, 401)
        get("/${randomUUID()}", null, 401)
        post("/", null, null, 401)
        put("/${randomUUID()}", null, null, 401)
        delete("/${randomUUID()}", null, 401)
    }

    @Test
    fun `CRUD managers have full access`() {
        val managerId = createManager(createVeoClientGroup())

        get("/", managerId, 200)
        get("/${randomUUID()}", managerId, 404)
        post("/", managerId, null, 400)
        put("/${randomUUID()}", managerId, null, 400)
        delete("/${randomUUID()}", managerId, 404)
    }

    @Test
    fun `readers have limited access`() {
        val readerId = createManager(createVeoClientGroup(), roles = listOf(READ))

        get("/", readerId, 200)
        get("/${randomUUID()}", readerId, 404)

        post("/", readerId, null, 403)
        put("/${randomUUID()}", readerId, null, 403)
        delete("/${randomUUID()}", readerId, 403)
    }

    @Test
    fun `creators have limited access`() {
        val creatorId = createManager(createVeoClientGroup(), roles = listOf(CREATE))

        post("/", creatorId, null, 400)

        get("/", creatorId, 403)
        get("/${randomUUID()}", creatorId, 403)
        put("/${randomUUID()}", creatorId, null, 403)
        delete("/${randomUUID()}", creatorId, 403)
    }

    @Test
    fun `updaters have limited access`() {
        val updaterId = createManager(createVeoClientGroup(), roles = listOf(UPDATE))

        put("/${randomUUID()}", updaterId, null, 400)

        get("/", updaterId, 403)
        get("/${randomUUID()}", updaterId, 403)
        post("/", updaterId, null, 403)
        delete("/${randomUUID()}", updaterId, 403)
    }

    @Test
    fun `deleters have limited access`() {
        val deleterId = createManager(createVeoClientGroup(), roles = listOf(DELETE))

        delete("/${randomUUID()}", deleterId, 404)

        get("/", deleterId, 403)
        get("/${randomUUID()}", deleterId, 403)
        post("/", deleterId, null, 403)
        put("/${randomUUID()}", deleterId, null, 403)
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
}
