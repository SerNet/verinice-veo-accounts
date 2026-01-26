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
package org.veo.accounts.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import org.veo.accounts.dtos.AccessGroupSurrogateIdSet
import org.veo.accounts.dtos.AssignableGroupSet
import org.veo.accounts.dtos.EmailAddress
import org.veo.accounts.dtos.Enabled
import org.veo.accounts.dtos.FirstName
import org.veo.accounts.dtos.Language
import org.veo.accounts.dtos.LastName
import org.veo.accounts.dtos.Username

@Schema(description = "Subset of veo user account data for account creation. Account ID is absent because it is generated.")
class CreateAccountDto(
    val username: Username,
    val emailAddress: EmailAddress,
    val firstName: FirstName,
    val lastName: LastName,
    val language: Language?,
    val groups: AssignableGroupSet,
    val accessGroups: AccessGroupSurrogateIdSet = AccessGroupSurrogateIdSet(),
    val enabled: Enabled,
)
