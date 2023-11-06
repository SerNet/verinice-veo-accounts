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
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.veo.accounts.AssignableGroup

@ArraySchema(
    arraySchema =
        Schema(
            description =
                "Groups that an account can be manually assigned to using veo-accounts. Assignable groups " +
                    "grant additional privileges. Automatically assigned groups (e.g. veo client membership group) are " +
                    "not included in this collection.",
        ),
    uniqueItems = true,
    schema = Schema(implementation = AssignableGroup::class),
)
data class AssignableGroupSet(
    @get:JsonValue val values: Set<AssignableGroup>,
) {
    val groupNames: List<String>
        get() = values.map { it.groupName }

    companion object {
        /** Creates an [AssignableGroupSet] from given group names. Non-assignable groups are omitted. */
        fun byGroupNames(groupNames: List<String>): AssignableGroupSet =
            groupNames
                .mapNotNull(AssignableGroup::byGroupNameOrNull)
                .toSet()
                .let { AssignableGroupSet(it) }
    }
}
