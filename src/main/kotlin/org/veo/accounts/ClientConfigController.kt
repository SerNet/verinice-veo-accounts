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
package org.veo.accounts

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.veo.accounts.auth.parseAccount
import org.veo.accounts.dtos.ClientConfigDto
import org.veo.accounts.keycloak.GroupService

@RestController
@RequestMapping("/client-config")
@SecurityRequirement(name = SECURITY_SCHEME_OAUTH)
class ClientConfigController(
    private val groupService: GroupService,
) {
    @Operation(description = "Get config for your client")
    @GetMapping
    fun get(auth: Authentication): ClientConfigDto =
        groupService
            .getClientGroup(auth.parseAccount().veoClient)
            .let { ClientConfigDto(it) }

    @Operation(description = "Update config for your client")
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun update(
        auth: Authentication,
        @Valid @RequestBody dto: ClientConfigDto,
    ) {
        groupService
            .updateClient(auth.parseAccount().veoClient, dto.toRoleNames())
    }
}
