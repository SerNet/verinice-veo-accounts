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
package org.veo.accounts

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.veo.accounts.auth.parseAccount
import org.veo.accounts.dtos.AccessGroupSurrogateId
import org.veo.accounts.dtos.request.AccessGroupRequestDto
import org.veo.accounts.dtos.response.AccessGroupCreatedDto
import org.veo.accounts.dtos.response.FullAccessGroupDto
import org.veo.accounts.keycloak.GroupService

@RestController
@RequestMapping("/access-groups")
@SecurityRequirement(name = SECURITY_SCHEME_OAUTH)
class AccessGroupController(
    private val groupService: GroupService,
) {
    @Operation(description = "Get all access groups.")
    @GetMapping
    fun getGroups(auth: Authentication): List<FullAccessGroupDto> =
        groupService
            .findAccessGroups(auth.parseAccount().veoClient)
            .map { FullAccessGroupDto(it) }

    @Operation(description = "Get a single access group.")
    @GetMapping("{id}")
    fun getGroup(
        auth: Authentication,
        @PathVariable("id") id: AccessGroupSurrogateId,
    ): FullAccessGroupDto =
        groupService
            .getAccessGroup(id, auth.parseAccount().veoClient)
            .let { FullAccessGroupDto(it) }

    @Operation(description = "Create an access group. Returns the new group's ID.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroup(
        auth: Authentication,
        @Valid
        @RequestBody
        dto: AccessGroupRequestDto,
    ): AccessGroupCreatedDto =
        dto
            .let { groupService.createAccessGroup(it.toAttributes(), auth.parseAccount().veoClient) }
            .let { id -> AccessGroupCreatedDto(id) }

    @Operation(description = "Update an access group.")
    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateGroup(
        auth: Authentication,
        @PathVariable("id") id: AccessGroupSurrogateId,
        @Valid
        @RequestBody
        dto: AccessGroupRequestDto,
    ): Unit = groupService.updateAccessGroup(id, dto.toAttributes(), auth.parseAccount().veoClient)

    @Operation(description = "Delete an access group.")
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGroup(
        auth: Authentication,
        @PathVariable("id") id: AccessGroupSurrogateId,
    ) = groupService.deleteAccessGroup(id, auth.parseAccount().veoClient)
}
