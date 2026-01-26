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
package org.veo.accounts

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
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
import org.veo.accounts.dtos.AccountId
import org.veo.accounts.dtos.request.CreateAccountDto
import org.veo.accounts.dtos.request.UpdateAccountDto
import org.veo.accounts.dtos.response.AccountCreatedDto
import org.veo.accounts.dtos.response.FullAccountDto
import org.veo.accounts.keycloak.AccountService

@RestController
@RequestMapping("/")
@SecurityRequirement(name = SECURITY_SCHEME_OAUTH)
class AccountController(
    private val accountService: AccountService,
) {
    @Operation(description = "Get all accounts.")
    @GetMapping
    fun getAccounts(auth: Authentication): List<FullAccountDto> =
        accountService
            .findAllAccounts(auth.parseAccount())
            .map { FullAccountDto(it) }

    @Operation(description = "Get a single account.")
    @GetMapping("{id}")
    fun getAccount(
        auth: Authentication,
        @PathVariable("id") id: AccountId,
    ): FullAccountDto =
        accountService
            .getAccount(id, auth.parseAccount())
            .let { FullAccountDto(it) }

    @Operation(description = "Create an account. Returns the new account's ID.")
    @PostMapping
    @ResponseStatus(CREATED)
    fun createAccount(
        auth: Authentication,
        @Valid
        @RequestBody
        dto: CreateAccountDto,
    ): AccountCreatedDto =
        dto
            .let { accountService.createAccount(it, auth.parseAccount()) }
            .let { id -> AccountCreatedDto(id) }

    @Operation(description = "Update an account.")
    @PutMapping("{id}")
    @ResponseStatus(NO_CONTENT)
    fun updateAccount(
        auth: Authentication,
        @PathVariable("id") id: AccountId,
        @Valid
        @RequestBody
        dto: UpdateAccountDto,
    ) = accountService.updateAccount(id, dto, auth.parseAccount())

    @Operation(description = "Delete an account.")
    @DeleteMapping("{id}")
    @ResponseStatus(NO_CONTENT)
    fun deleteAccount(
        auth: Authentication,
        @PathVariable("id") id: AccountId,
    ) = accountService.deleteAccount(id, auth.parseAccount())
}
