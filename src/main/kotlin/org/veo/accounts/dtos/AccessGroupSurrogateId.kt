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
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.UUID

private const val KEYCLOAK_GROUP_NAME_PREFIX = "access_group_"

/**
 * This is NOT the internal keycloak group ID, but a surrogate ID baked into the keycloak group name.
 */
@Schema(
    description = "Generated ID for an access group.",
    type = "text",
    example = "d267ef91-aaff-4c4f-a09f-612f7b87d865",
)
data class AccessGroupSurrogateId(
    @get:JsonValue
    val value: UUID = UUID.randomUUID(),
) {
    override fun toString() = value.toString()

    val groupName: String = "$KEYCLOAK_GROUP_NAME_PREFIX$value"

    companion object {
        fun byGroupName(keycloakGroupName: String): AccessGroupSurrogateId? =
            Regex("$KEYCLOAK_GROUP_NAME_PREFIX(.+)")
                .find(keycloakGroupName)
                ?.groupValues[1]
                ?.let { AccessGroupSurrogateId(UUID.fromString(it)) }
    }
}

@Component
private class AccessGroupIdConverter : Converter<String, AccessGroupSurrogateId> {
    override fun convert(source: String): AccessGroupSurrogateId = AccessGroupSurrogateId(UUID.fromString(source))
}
