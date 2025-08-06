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
package org.veo.accounts.dtos

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.keycloak.representations.idm.GroupRepresentation
import org.veo.accounts.Role

@Schema(description = "Configuration parameters for a veo client")
class ClientConfigDto(
    val restrictUnitAccess: Boolean,
) {
    constructor(clientGroup: GroupRepresentation) : this(
        clientGroup.realmRoles.contains(Role.UNIT_ACCESS_RESTRICTION.roleName),
    )

    @JsonIgnore
    fun toRoleNames(): List<String> = if (restrictUnitAccess) listOf(Role.UNIT_ACCESS_RESTRICTION.roleName) else emptyList()
}
