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
package org.veo.accounts.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import org.veo.accounts.dtos.AccessGroupDisplayName
import org.veo.accounts.dtos.UnitAccessRights

@Schema(
    description = "Subset of veo access group data for creating and updating access groups. Group ID is absent because it is generated.",
)
class AccessGroupRequestDto(
    val name: AccessGroupDisplayName,
    val units: UnitAccessRights = UnitAccessRights(),
) {
    fun toAttributes(): Map<String, List<String>> = name.toAttributes() + units.toAttributes()
}
