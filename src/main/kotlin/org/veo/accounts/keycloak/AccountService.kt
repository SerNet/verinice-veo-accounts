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
package org.veo.accounts.keycloak

import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.veo.accounts.auth.AuthenticatedAccount
import org.veo.accounts.dtos.AccountId
import org.veo.accounts.dtos.AssignableGroupSet
import org.veo.accounts.dtos.request.CreateAccountDto
import org.veo.accounts.dtos.request.UpdateAccountDto
import org.veo.accounts.exceptions.ConflictException
import org.veo.accounts.exceptions.ForbiddenOperationException
import org.veo.accounts.exceptions.ResourceNotFoundException
import javax.ws.rs.ClientErrorException
import javax.ws.rs.NotFoundException

/** Performs account-related actions on keycloak. Do not perform such actions without this service. */
@Component
class AccountService(
    @Value("\${veo.accounts.keycloak.userSuperGroupName}")
    private val userSuperGroupName: String,
    private val facade: KeycloakFacade
) {
    fun findAllAccounts(authAccount: AuthenticatedAccount): List<UserRepresentation> = facade.perform {
        groups()
            .group(getGroupId(authAccount.veoClient.groupName))
            .members()
    }

    fun getAccount(id: AccountId, authAccount: AuthenticatedAccount): UserRepresentation = facade.perform {
        users().get(id.toString())
            .let { userResource ->
                try {
                    userResource.toRepresentation().apply {
                        groups = userResource.groups().map { it.name } // groups must be fetched separately
                    }
                } catch (_: NotFoundException) {
                    throw ResourceNotFoundException()
                }
            }
            .apply { if (!groups.contains(authAccount.veoClient.groupName)) throw ResourceNotFoundException() }
    }

    fun createAccount(dto: CreateAccountDto, authAccount: AuthenticatedAccount): String = facade.perform {
        dto
            .toUser(authAccount)
            .let { users().create(it) }
            .apply { if (status == 409) throw ConflictException("Username or email address already taken") }
            .apply { check(status == 201) { "Unexpected user creation response $status" } }
            .let(facade::parseResourceId)
    }

    fun updateAccount(id: AccountId, dto: UpdateAccountDto, authAccount: AuthenticatedAccount) =
        facade.perform {
            getAccount(id, authAccount)
                .apply { update(dto) }
                .also {
                    try {
                        users().get(id.toString()).update(it)
                    } catch (ex: ClientErrorException) {
                        if (ex.response.status == 409) {
                            throw ConflictException("Email address already taken")
                        }
                        throw ex
                    }
                }
                .also { user ->
                    dto.groups
                        .groupNames
                        .filter { !user.groups.contains(it) }
                        .forEach { users().get(user.id).joinGroup(getGroupId(it)) }
                }
                .also { user ->
                    AssignableGroupSet.byGroupNames(user.groups)
                        .values
                        .filter { !dto.groups.values.contains(it) }
                        .forEach { users().get(user.id).leaveGroup(getGroupId(it.groupName)) }
                }
        }

    fun deleteAccount(id: AccountId, authAccount: AuthenticatedAccount) = facade.perform {
        if (id == authAccount.id) throw ForbiddenOperationException("Account cannot self-destruct")
        getAccount(id, authAccount)
            .also { users().delete(id.toString()) }
            .run { }
    }

    private fun CreateAccountDto.toUser(authAccount: AuthenticatedAccount) = UserRepresentation().also { user ->
        user.username = username.value
        user.email = emailAddress.value
        user.firstName = firstName.value
        user.lastName = lastName.value
        user.groups = groups.groupNames.map(::getUserGroupPath) +
            getUserGroupPath("veo-user") +
            authAccount.veoClient.groupName
    }

    private fun UserRepresentation.update(dto: UpdateAccountDto) {
        dto.emailAddress.value
            .let {
                if (it != email) {
                    email = it
                    isEmailVerified = false
                }
            }
        firstName = dto.firstName.value
        lastName = dto.lastName.value
    }

    private fun getUserGroupPath(groupName: String): String = "$userSuperGroupName/$groupName"

    private fun RealmResource.getGroupId(groupName: String): String = groups()
        .groups(groupName, 0, 1)
        .first()
        .let { listOf(it) + it.subGroups }
        .firstOrNull { it.name == groupName }
        ?.id
        ?: throw IllegalStateException("Group with name '$groupName' not found")
}
