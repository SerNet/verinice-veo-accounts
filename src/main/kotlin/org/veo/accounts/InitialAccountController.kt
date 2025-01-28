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
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.veo.accounts.dtos.request.CreateInitialAccountDto
import org.veo.accounts.dtos.response.AccountCreatedDto
import org.veo.accounts.keycloak.AccountService

@RestController
@RequestMapping("initial")
@SecurityRequirement(name = SECURITY_SCHEME_CLIENT_INIT_API_KEY)
class InitialAccountController(
    private val accountService: AccountService,
) {
    @Operation(
        description = "Create the initial account for a client. Returns the new account's ID.",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "Initial account created",
            ),
            ApiResponse(
                responseCode = "409",
                description = "Username or email address already taken",
            ),
            ApiResponse(
                responseCode = "409",
                description = "Accounts already exist in target veo client",
            ),
            ApiResponse(
                responseCode = "422",
                description = "Target veo client does not exist",
            ),
        ],
    )
    @PostMapping
    @ResponseStatus(CREATED)
    fun createInitialAccount(
        @Valid
        @RequestBody
        dto: CreateInitialAccountDto,
    ): AccountCreatedDto =
        dto
            .let { accountService.createInitialAccount(it) }
            .let { id -> AccountCreatedDto(id) }
}
