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
package org.veo.accounts.dtos.response

import io.swagger.v3.oas.annotations.media.Schema
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.veo.accounts.dtos.AccountId
import org.veo.accounts.dtos.AssignableGroupSet
import org.veo.accounts.dtos.EmailAddress
import org.veo.accounts.dtos.Enabled
import org.veo.accounts.dtos.FirstName
import org.veo.accounts.dtos.LastName
import org.veo.accounts.dtos.Link
import org.veo.accounts.dtos.Username
import java.net.URI

@Schema(description = "Veo user account that is a member of the authenticated user's veo client group")
class FullAccountDto(
    val id: AccountId,
    val username: Username,
    val emailAddress: EmailAddress?,
    val firstName: FirstName?,
    val lastName: LastName?,
    val groups: AssignableGroupSet,
    val enabled: Enabled,
) {
    constructor(user: UserRepresentation) : this(
        AccountId(user.id),
        Username(user.username),
        user.email?.let { EmailAddress(it) },
        user.firstName?.let { FirstName(it) },
        user.lastName?.let { LastName(it) },
        AssignableGroupSet.byGroupNames(user.groups),
        Enabled(user.isEnabled),
    )
    val _self = Link(URI.create("${ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()}/$id"))
}
