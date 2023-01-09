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

import mu.KotlinLogging.logger
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.veo.accounts.auth.AuthenticatedAccount
import org.veo.accounts.auth.VeoClient
import org.veo.accounts.dtos.AccountId
import org.veo.accounts.dtos.AssignableGroupSet
import org.veo.accounts.dtos.request.CreateAccountDto
import org.veo.accounts.dtos.request.UpdateAccountDto
import org.veo.accounts.exceptions.ConflictException
import org.veo.accounts.exceptions.ExceedingMaxUsersException
import org.veo.accounts.exceptions.ResourceNotFoundException
import javax.ws.rs.ClientErrorException
import javax.ws.rs.NotFoundException

private val log = logger {}

private const val ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED = "veo-accounts.deactivated"

/** Performs account-related actions on keycloak. Do not perform such actions without this service. */
@Component
class AccountService(
    @Value("\${veo.accounts.keycloak.userSuperGroupName}")
    private val userSuperGroupName: String,
    private val facade: KeycloakFacade,
    @Value("\${veo.accounts.keycloak.mailing.enabled}")
    private val mailingEnabled: Boolean,
) {
    fun findAllAccounts(authAccount: AuthenticatedAccount): List<UserRepresentation> = facade.perform {
        findAccounts(authAccount)
            // Self-management is not supported
            .filter { it.id != authAccount.id.toString() }
            .onEach { loadGroups(it) }
    }

    fun getAccount(id: AccountId, authAccount: AuthenticatedAccount): UserRepresentation = facade.perform {
        // Self-management is not supported
        if (id == authAccount.id) throw ResourceNotFoundException()
        users().get(id.toString())
            .let { userResource ->
                try {
                    userResource.toRepresentation()
                } catch (_: NotFoundException) {
                    throw ResourceNotFoundException()
                }
            }
            .also { loadGroups(it) }
            .apply { if (!groups.contains(authAccount.veoClient.groupName)) throw ResourceNotFoundException() }
    }

    fun createAccount(dto: CreateAccountDto, authAccount: AuthenticatedAccount): AccountId =
        performSynchronized(authAccount) {
            dto
                .apply {
                    if (enabled.value) {
                        checkMaxUsersNotExhausted(authAccount)
                    }
                }
                .let { dtoToUser(it, authAccount) }
                .let { users().create(it) }
                .apply { if (status == 409) throw ConflictException("Username or email address already taken") }
                .apply { check(status == 201) { "Unexpected user creation response $status" } }
                .let(facade::parseResourceId)
                .also { sendEmail(it) }
                .let { AccountId(it) }
        }

    fun updateAccount(id: AccountId, dto: UpdateAccountDto, authAccount: AuthenticatedAccount) =
        performSynchronized(authAccount) {
            getAccount(id, authAccount)
                .apply {
                    if (!isEnabled && dto.enabled.value) {
                        checkMaxUsersNotExhausted(authAccount)
                    }
                }
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
                .run { if (!isEmailVerified) sendEmail(id.toString()) }
        }

    fun deleteAccount(id: AccountId, authAccount: AuthenticatedAccount) = performSynchronized(authAccount) {
        getAccount(id, authAccount)
            .also { users().delete(id.toString()) }
            .run { }
    }

    fun deactivateClient(veoClient: VeoClient) = performSynchronized(veoClient) {
        groups().group(getGroupId(veoClient.groupName))
            .apply {
                toRepresentation()
                    .singleAttribute(ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED, "true")
                    .let(::update)
            }
            .members()
            .map { users().get(it.id) }
            .filter { userResource -> userResource.groups().any { it.name == "veo-user" } }
            .forEach {
                it.leaveGroup(getGroupId("veo-user"))
            }
    }

    fun activateClient(veoClient: VeoClient) = performSynchronized(veoClient) {
        groups().group(getGroupId(veoClient.groupName))
            .apply {
                toRepresentation()
                    .apply { attributes.remove(ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED) }
                    .let(::update)
            }
            .members()
            .forEach {
                users().get(it.id).joinGroup(getGroupId("veo-user"))
            }
    }

    fun deleteClient(client: VeoClient) = facade.perform {
        log.info("Deleting veo client group ${client.groupName}")
        tryDeleteGroup(getGroupId(client.groupName))
    }

    private fun <T> performSynchronized(authAccount: AuthenticatedAccount, block: RealmResource.() -> T): T =
        performSynchronized(authAccount.veoClient, block)

    private fun <T> performSynchronized(client: VeoClient, block: RealmResource.() -> T): T = facade.perform {
        synchronized(client.groupName.intern()) {
            block()
        }
    }

    private fun RealmResource.findAccounts(authAccount: AuthenticatedAccount): List<UserRepresentation> =
        groups()
            .group(getGroupId(authAccount.veoClient.groupName))
            .members()

    private fun RealmResource.checkMaxUsersNotExhausted(authAccount: AuthenticatedAccount) =
        getMaxUsers(authAccount).let {
            if (countEnabledUsers(authAccount) >= it) {
                throw ExceedingMaxUsersException(it)
            }
        }

    private fun RealmResource.getMaxUsers(authAccount: AuthenticatedAccount): Int = groups()
        .group(getGroupId(authAccount.veoClient.groupName))
        .toRepresentation()
        .attributes["maxUsers"]
        ?.firstOrNull()
        ?.toInt()
        ?: throw IllegalStateException("maxUsers is not defined in group ${authAccount.veoClient.groupName}")

    private fun RealmResource.countEnabledUsers(authAccount: AuthenticatedAccount): Int =
        findAccounts(authAccount)
            .filter { it.isEnabled }
            .size

    private fun RealmResource.dtoToUser(dto: CreateAccountDto, authAccount: AuthenticatedAccount) = UserRepresentation().apply {
        username = dto.username.value
        email = dto.emailAddress.value
        firstName = dto.firstName.value
        lastName = dto.lastName.value
        groups = dto.groups.groupNames.map(::getUserGroupPath) +
            authAccount.veoClient.groupName +
            if (groupActivated(authAccount.veoClient)) listOf(getUserGroupPath("veo-user")) else emptyList()
        isEnabled = dto.enabled.value
    }

    private fun RealmResource.groupActivated(veoClient: VeoClient): Boolean =
        groups().group(getGroupId(veoClient.groupName))
            .toRepresentation()
            .attributes[ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED] != listOf("true")

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
        isEnabled = dto.enabled.value
    }

    private fun getUserGroupPath(groupName: String): String = "$userSuperGroupName/$groupName"

    private fun RealmResource.getGroupId(groupName: String): String = groups()
        .groups(groupName, 0, 1)
        .first()
        .let { listOf(it) + it.subGroups }
        .firstOrNull { it.name == groupName }
        ?.id
        ?: throw IllegalStateException("Group with name '$groupName' not found")

    private fun RealmResource.sendEmail(accountId: String) = accountId
        .let { users().get(accountId).toRepresentation() }
        .apply { if (!isEnabled) return }
        .run { requiredActions + if (!isEmailVerified) listOf("VERIFY_EMAIL") else emptyList() }
        .also { log.debug { "Determined email actions for user $accountId: $it" } }
        .let { if (mailingEnabled) users().get(accountId).executeActionsEmail(it) }

    private fun RealmResource.loadGroups(user: UserRepresentation) {
        user.apply {
            groups = users().get(id.toString()).groups().map { it.name }
        }
    }
}
