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

import org.veo.accounts.dtos.AccessGroupSurrogateId
import org.veo.accounts.dtos.AccessGroupSurrogateIdSet
import org.veo.accounts.dtos.AccountId
import org.veo.accounts.dtos.AssignableGroupSet
import org.veo.accounts.dtos.EmailAddress
import org.veo.accounts.dtos.Enabled
import org.veo.accounts.dtos.FirstName
import org.veo.accounts.dtos.Language
import org.veo.accounts.dtos.LastName
import org.veo.accounts.dtos.Username
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.dtos.request.CreateAccountDto
import org.veo.accounts.dtos.request.CreateInitialAccountDto
import org.veo.accounts.dtos.request.UpdateAccountDto
import org.veo.accounts.dtos.response.FullAccountDto
import java.util.UUID
import java.util.UUID.randomUUID

fun fullAccountDto(
    id: AccountId = AccountId(randomUUID().toString()),
    username: String = "ksmith",
    emailAddress: String = "katie@test.test",
    firstName: String = "Katie",
    lastName: String = "Smith",
    language: String? = "en",
    groups: Set<AssignableGroup> = emptySet(),
    accessGroups: Set<AccessGroupSurrogateId> = emptySet(),
    enabled: Boolean = true,
) = FullAccountDto(
    id,
    Username(username),
    EmailAddress(emailAddress),
    FirstName(firstName),
    LastName(lastName),
    language?.let { Language(it) },
    AssignableGroupSet(groups),
    AccessGroupSurrogateIdSet(accessGroups),
    Enabled(enabled),
)

fun createAccountDto(
    emailAddress: String = "katie@test.test",
    username: String = "ksmith",
    firstName: String = "Katie",
    lastName: String = "Smith",
    language: String? = "en",
    groups: Set<AssignableGroup> = emptySet(),
    accessGroups: Set<AccessGroupSurrogateId> = emptySet(),
    enabled: Boolean = true,
) = CreateAccountDto(
    Username(username),
    EmailAddress(emailAddress),
    FirstName(firstName),
    LastName(lastName),
    language?.let { Language(it) },
    AssignableGroupSet(groups),
    AccessGroupSurrogateIdSet(accessGroups),
    Enabled(enabled),
)

fun createInitialAccountDto(
    veoClientId: VeoClientId = VeoClientId(UUID.randomUUID()),
    emailAddress: String = "katie@test.test",
    username: String = "ksmith",
    firstName: String = "Katie",
    lastName: String = "Smith",
    language: String? = "en",
) = CreateInitialAccountDto(
    veoClientId,
    Username(username),
    EmailAddress(emailAddress),
    FirstName(firstName),
    LastName(lastName),
    language?.let { Language(it) },
)

fun updateAccountDto(
    emailAddress: String = "katie@test.test",
    firstName: String = "Katie",
    lastName: String = "Smith",
    language: String? = "en",
    groups: Set<AssignableGroup> = emptySet(),
    accessGroups: Set<AccessGroupSurrogateId> = emptySet(),
    enabled: Boolean = true,
) = UpdateAccountDto(
    EmailAddress(emailAddress),
    FirstName(firstName),
    LastName(lastName),
    language?.let {
        Language(it)
    },
    AssignableGroupSet(groups),
    AccessGroupSurrogateIdSet(accessGroups),
    Enabled(enabled),
)
