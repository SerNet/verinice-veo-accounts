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

import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.NotFoundException
import mu.KotlinLogging.logger
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.veo.accounts.AssignableGroup.VEO_WRITE_ACCESS
import org.veo.accounts.auth.AuthenticatedAccount
import org.veo.accounts.dtos.AccessGroupSurrogateId
import org.veo.accounts.dtos.AccessGroupSurrogateIdSet
import org.veo.accounts.dtos.AccountId
import org.veo.accounts.dtos.AssignableGroupSet
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.dtos.request.CreateAccountDto
import org.veo.accounts.dtos.request.CreateInitialAccountDto
import org.veo.accounts.dtos.request.UpdateAccountDto
import org.veo.accounts.exceptions.ConflictException
import org.veo.accounts.exceptions.ExceedingMaxUsersException
import org.veo.accounts.exceptions.ResourceNotFoundException

private val log = logger {}

const val ATTRIBUTE_LOCALE = "locale"

/** Performs account-related actions on keycloak. Do not perform such actions without this service. */
@Component
class AccountService(
    @Value("\${veo.accounts.keycloak.userSuperGroupName}")
    private val userSuperGroupName: String,
    private val facade: KeycloakFacade,
    private val groupService: GroupService,
    private val licenseService: LicenseService,
    @Value("\${veo.accounts.keycloak.mailing.enabled}")
    private val mailingEnabled: Boolean,
    @Value("\${veo.accounts.keycloak.mailing.actionsRedirectUrl}")
    private val mailActionsRedirectUrl: String,
    @Value("\${veo.accounts.keycloak.clients.auth.name}")
    private val userAuthKeycloakClient: String,
) {
    fun findAllAccounts(authAccount: AuthenticatedAccount): List<UserRepresentation> =
        facade.perform {
            findAccounts(authAccount)
                // Self-management is not supported
                .filter { it.id != authAccount.id.toString() }
                .onEach { loadGroups(it) }
        }

    fun getAccount(
        id: AccountId,
        authAccount: AuthenticatedAccount,
    ): UserRepresentation =
        facade.perform {
            // Self-management is not supported
            if (id == authAccount.id) throw ResourceNotFoundException()
            users()
                .get(id.toString())
                .let { userResource ->
                    try {
                        userResource.toRepresentation()
                    } catch (_: NotFoundException) {
                        throw ResourceNotFoundException()
                    }
                }.also { loadGroups(it) }
                .apply { if (!groups.contains(authAccount.veoClient.groupName)) throw ResourceNotFoundException() }
        }

    fun createInitialAccount(dto: CreateInitialAccountDto): AccountId =
        facade.performSynchronized(dto.clientId) {
            checkGlobalMaxUsersNotExhausted()
            groupService.getClientGroup(dto.clientId)
            if (findAccounts(dto.clientId).isNotEmpty()) {
                throw ConflictException("Target client already contains accounts, cannot create initial account")
            }

            dtoToInitialUser(dto)
                .also { log.info { "Creating initial account ${it.username} for ${dto.clientId}" } }
                .let { createAccount(it) }
        }

    fun createAccount(
        dto: CreateAccountDto,
        authAccount: AuthenticatedAccount,
    ): AccountId =
        facade.performSynchronized(authAccount) {
            licenseService.getInstalledLicense()
            dto
                .apply {
                    if (enabled.value) {
                        checkMaxUsersNotExhausted(authAccount)
                        checkGlobalMaxUsersNotExhausted()
                    }
                }.let { dtoToUser(it, authAccount) }
                .also { log.info { "Creating new account ${it.username} in ${authAccount.veoClient}" } }
                .let { createAccount(it) }
        }

    private fun RealmResource.createAccount(userRepresentation: UserRepresentation): AccountId =
        userRepresentation
            .let { users().create(it) }
            .apply { if (status == 409) throw ConflictException("Username or email address already taken") }
            .apply { check(status == 201) { "Unexpected user creation response $status" } }
            .let(facade::parseResourceId)
            .also { sendEmail(it) }
            .let { AccountId(it) }

    fun updateAccount(
        id: AccountId,
        dto: UpdateAccountDto,
        authAccount: AuthenticatedAccount,
    ) = facade.performSynchronized(authAccount) {
        getAccount(id, authAccount)
            .apply {
                if (!isEnabled && dto.enabled.value) {
                    checkMaxUsersNotExhausted(authAccount)
                    checkGlobalMaxUsersNotExhausted()
                }
            }.also { log.info { "Updating account ${it.username} in ${authAccount.veoClient}" } }
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
            }.also { user ->
                dto.groups
                    .values
                    .filter { !user.groups.contains(it.groupName) }
                    .forEach { users().get(user.id).joinGroup(groupService.getAssignableGroup(it).id) }
            }.also { user ->
                AssignableGroupSet
                    .byGroupNames(user.groups)
                    .values
                    .filter { !dto.groups.values.contains(it) }
                    .forEach { users().get(user.id).leaveGroup(groupService.getAssignableGroup(it).id) }
            }.also { user ->
                dto.accessGroups
                    .filter { !user.groups.contains(it.groupName) }
                    .let { groupService.getAccessGroups(it, authAccount.veoClient) }
                    .forEach { users().get(user.id).joinGroup(it.id) }
            }.also { user ->
                AccessGroupSurrogateIdSet
                    .byGroupNames(user.groups)
                    .filter { !dto.accessGroups.contains(it) }
                    .let { groupService.getAccessGroups(it, authAccount.veoClient) }
                    .forEach { users().get(user.id).leaveGroup(it.id) }
            }.run { if (!isEmailVerified) sendEmail(id.toString()) }
    }

    fun deleteAccount(
        id: AccountId,
        authAccount: AuthenticatedAccount,
    ) = facade.performSynchronized(authAccount) {
        getAccount(id, authAccount)
            .also { log.info { "Deleting account ${it.username} in ${authAccount.veoClient}" } }
            .also { users().delete(id.toString()) }
            .run { }
    }

    private fun RealmResource.findAccounts(authAccount: AuthenticatedAccount): List<UserRepresentation> =
        findAccounts(authAccount.veoClient)

    private fun RealmResource.findAccounts(veoClient: VeoClientId): List<UserRepresentation> =
        groups()
            .group(groupService.getClientGroup(veoClient, true).id)
            .members()

    private fun RealmResource.checkMaxUsersNotExhausted(authAccount: AuthenticatedAccount) =
        getMaxUsers(authAccount).let {
            if (countEnabledUsers(authAccount) >= it) {
                throw ExceedingMaxUsersException(it)
            }
        }

    private fun RealmResource.checkGlobalMaxUsersNotExhausted() {
        val licensedMax = licenseService.getLicensedTotalUsers()
        val currentlyEnabled =
            users()
                .search("", 0, Int.MAX_VALUE)
                .count { it.isEnabled }
        if (currentlyEnabled >= licensedMax) {
            throw ExceedingMaxUsersException(licensedMax)
        }
    }

    private fun getMaxUsers(authAccount: AuthenticatedAccount): Int =
        groupService
            .getClientGroup(authAccount.veoClient)
            .attributes["maxUsers"]
            ?.firstOrNull()
            ?.toInt()
            ?: throw IllegalStateException("maxUsers is not defined in group ${authAccount.veoClient.groupName}")

    private fun RealmResource.countEnabledUsers(authAccount: AuthenticatedAccount): Int =
        findAccounts(authAccount)
            .filter { it.isEnabled }
            .size

    private fun dtoToUser(
        dto: CreateAccountDto,
        authAccount: AuthenticatedAccount,
    ) = UserRepresentation().apply {
        username = dto.username.value
        email = dto.emailAddress.value
        firstName = dto.firstName.value
        lastName = dto.lastName.value
        singleAttribute<UserRepresentation>(ATTRIBUTE_LOCALE, dto.language?.value)
        groups = getGroupsForNewAccount(authAccount.veoClient, dto.groups, dto.accessGroups)
        isEnabled = dto.enabled.value
    }

    private fun dtoToInitialUser(dto: CreateInitialAccountDto) =
        UserRepresentation().apply {
            username = dto.username.value
            email = dto.emailAddress.value
            firstName = dto.firstName.value
            lastName = dto.lastName.value
            singleAttribute<UserRepresentation>(ATTRIBUTE_LOCALE, dto.language?.value)
            groups = getGroupsForNewAccount(dto.clientId, AssignableGroupSet(setOf(VEO_WRITE_ACCESS)), emptySet(), true)
            isEnabled = true
        }

    private fun getGroupsForNewAccount(
        veoClient: VeoClientId,
        assignableGroups: AssignableGroupSet,
        accessGroups: Collection<AccessGroupSurrogateId>,
        isAccountManager: Boolean = false,
    ) = mutableListOf<String>()
        .apply { addAll(assignableGroups.groupNames.map(::getUserGroupPath)) }
        .apply { addAll(groupService.getAccessGroups(accessGroups, veoClient).map { it.path }) }
        .apply { add(veoClient.groupName) }
        .apply { if (groupService.clientIsActive(veoClient)) add(getUserGroupPath("veo-user")) }
        .apply { if (isAccountManager) add(getUserGroupPath("veo-accountmanagers")) }

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
        singleAttribute<UserRepresentation>(ATTRIBUTE_LOCALE, dto.language?.value)
    }

    private fun getUserGroupPath(groupName: String): String = "$userSuperGroupName/$groupName"

    private fun RealmResource.sendEmail(accountId: String) =
        accountId
            .let { users().get(accountId).toRepresentation() }
            .apply { if (!isEnabled) return }
            .run { requiredActions + if (!isEmailVerified) listOf("VERIFY_EMAIL") else emptyList() }
            .also { log.debug { "Determined email actions for user $accountId: $it" } }
            .also { log.debug { "Mailing keycloak client: $userAuthKeycloakClient" } }
            .also { log.debug { "Mailing actions redirect URL: $mailActionsRedirectUrl" } }
            .let { actions ->
                if (mailingEnabled) users().get(accountId).executeActionsEmail(userAuthKeycloakClient, mailActionsRedirectUrl, actions)
            }

    private fun RealmResource.loadGroups(user: UserRepresentation) {
        user.apply {
            groups = users().get(id.toString()).groups().map { it.name }
        }
    }

    fun getNumberOfUsers() = facade.perform { users().count() }
}
