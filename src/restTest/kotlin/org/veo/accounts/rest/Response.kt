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

import org.springframework.http.ResponseEntity
import org.veo.accounts.asListOfMaps
import org.veo.accounts.asMap
import tools.jackson.module.kotlin.jacksonObjectMapper

private val objectMapper = jacksonObjectMapper()

class Response(
    private val entity: ResponseEntity<String>,
) {
    val bodyAsMap get() = parseBody().asMap()
    val bodyAsListOfMaps get() = parseBody().asListOfMaps()
    val rawBody = entity.body
    val statusCode = entity.statusCode.value()

    fun getHeader(name: String): String? = entity.headers[name]?.firstOrNull()

    private fun parseBody(): Any = objectMapper.readValue(rawBody, Any::class.java)
}
