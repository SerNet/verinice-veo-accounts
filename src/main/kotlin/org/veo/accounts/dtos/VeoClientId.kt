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
package org.veo.accounts.dtos

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

private const val uuidPattern = """[a-fA-F\d]{8}(?:-[a-fA-F\d]{4}){3}-[a-fA-F\d]{12}"""
private const val clientGroupPrefix = "veo_client:"
private val clientGroupPathRegex = Regex("^/$clientGroupPrefix($uuidPattern)$")

/**
 * Not to be confused with the keycloak client. Each keycloak user account can be assigned to one veo client with a
 * group mapping.
 */
@Schema(
    description = "UUID of a veo client. Each account is assigned to a client. Account managers can only manage accounts within their own client.",
)
data class VeoClientId(
    @get:JsonValue
    val clientId: UUID,
) {
    val groupName = "$clientGroupPrefix$clientId"
    val path = "/$groupName"
    companion object {
        fun tryParse(groupPath: String): VeoClientId? = groupPath
            .let { clientGroupPathRegex.matchEntire(it) }
            ?.let { VeoClientId(UUID.fromString(it.groups[1]!!.value)) }
    }
}
