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
package org.veo.accounts

import org.veo.accounts.dtos.AccountId
import org.veo.accounts.dtos.AssignableGroupSet
import org.veo.accounts.dtos.EmailAddress
import org.veo.accounts.dtos.Enabled
import org.veo.accounts.dtos.FirstName
import org.veo.accounts.dtos.LastName
import org.veo.accounts.dtos.Username
import org.veo.accounts.dtos.request.CreateAccountDto
import org.veo.accounts.dtos.request.UpdateAccountDto
import org.veo.accounts.dtos.response.FullAccountDto
import java.util.UUID.randomUUID

fun fullAccountDto(
    id: AccountId = AccountId(randomUUID().toString()),
    username: String = "ksmith",
    emailAddress: String = "katie@test.test",
    firstName: String = "Katie",
    lastName: String = "Smith",
    groups: Set<AssignableGroup> = emptySet(),
    enabled: Boolean = true
) =
    FullAccountDto(
        id,
        Username(username),
        EmailAddress(emailAddress),
        FirstName(firstName),
        LastName(lastName),
        AssignableGroupSet(groups),
        Enabled(enabled)
    )

fun createAccountDto(
    emailAddress: String = "katie@test.test",
    username: String = "ksmith",
    firstName: String = "Katie",
    lastName: String = "Smith",
    groups: Set<AssignableGroup> = emptySet(),
    enabled: Boolean = true
) =
    CreateAccountDto(
        Username(username),
        EmailAddress(emailAddress),
        FirstName(firstName),
        LastName(lastName),
        AssignableGroupSet(groups),
        Enabled(enabled)
    )

fun updateAccountDto(
    emailAddress: String = "katie@test.test",
    firstName: String = "Katie",
    lastName: String = "Smith",
    groups: Set<AssignableGroup> = emptySet(),
    enabled: Boolean = true
) =
    UpdateAccountDto(EmailAddress(emailAddress), FirstName(firstName), LastName(lastName), AssignableGroupSet(groups), Enabled(enabled))
