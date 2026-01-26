/*
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
package org.veo.accounts.dtos.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.keycloak.representations.idm.GroupRepresentation
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.veo.accounts.dtos.AccessGroupDisplayName
import org.veo.accounts.dtos.AccessGroupSurrogateId
import org.veo.accounts.dtos.Link
import org.veo.accounts.dtos.UnitAccessRights
import java.net.URI

@Schema(description = "Access group that exists in the authenticated user's veo client group and grants access to certain units.")
class FullAccessGroupDto(
    val id: AccessGroupSurrogateId,
    val name: AccessGroupDisplayName,
    val units: UnitAccessRights,
) {
    constructor(group: GroupRepresentation) : this(
        AccessGroupSurrogateId.byGroupName(group.name)!!,
        AccessGroupDisplayName.byAttributes(group.attributes),
        UnitAccessRights.byAttributes(group.attributes),
    )

    @JsonProperty(value = "_self")
    val self = Link(URI.create("${ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()}/access-groups/$id"))
}
