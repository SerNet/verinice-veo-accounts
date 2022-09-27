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
package org.veo.accounts.auth

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.veo.accounts.dtos.AccountId
import java.util.UUID

private const val uuidPattern = """[a-fA-F\d]{8}(?:-[a-fA-F\d]{4}){3}-[a-fA-F\d]{12}"""
private val clientGroupRegex = Regex("^/(veo_client:($uuidPattern))$")

fun Authentication.parseAccount(): AuthenticatedAccount = AuthenticatedAccount(AccountId(name), getVeoClient())

private fun Authentication.getVeoClient(): VeoClient = token()
    .getClaimAsStringList("groups")!!
    .mapNotNull { clientGroupRegex.matchEntire(it) }
    .also { require(it.size == 1) { "Expected 1 client for the account. Got ${it.size}." } }
    .first()
    .let { VeoClient(it.groups[1]!!.value, UUID.fromString(it.groups[2]!!.value)) }

private fun Authentication.token(): Jwt =
    (this as JwtAuthenticationToken).token
