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

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@ArraySchema(
    arraySchema =
        Schema(
            description =
                "IDs of associated access groups",
        ),
    uniqueItems = true,
    schema = Schema(implementation = AccessGroupSurrogateId::class),
)
class AccessGroupSurrogateIdSet(
    values: Collection<AccessGroupSurrogateId> = emptySet(),
) : HashSet<AccessGroupSurrogateId>(values) {
    companion object {
        /** Creates an [AccessGroupSurrogateIdSet] from given group names. Non-assignable groups are omitted. */
        fun byGroupNames(groupNames: List<String?>): AccessGroupSurrogateIdSet =
            AccessGroupSurrogateIdSet(
                groupNames
                    .mapNotNull { AccessGroupSurrogateId.byGroupName(it!!) }
                    .toSet(),
            )
    }
}
