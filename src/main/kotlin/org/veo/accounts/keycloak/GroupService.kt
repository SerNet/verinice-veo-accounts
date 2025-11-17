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
package org.veo.accounts.keycloak

import mu.KotlinLogging.logger
import org.keycloak.admin.client.resource.RoleScopeResource
import org.keycloak.representations.idm.GroupRepresentation
import org.keycloak.representations.idm.RoleRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.veo.accounts.AssignableGroup
import org.veo.accounts.dtos.AccessGroupSurrogateId
import org.veo.accounts.dtos.UnitAccessRights
import org.veo.accounts.dtos.UnitId
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.exceptions.ExceedingMaxClientsException
import org.veo.accounts.exceptions.ResourceNotFoundException
import org.veo.accounts.exceptions.UnprocessableDtoException

const val CLIENT_GROUP_PREFIX = "veo_client:"
private const val ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED = "veo-accounts.deactivated"

private val log = logger {}

@Component
class GroupService(
    private val facade: KeycloakFacade,
    private val licenseService: LicenseService,
    @Value("\${veo.accounts.developer.mode.enabled}")
    private val isDeveloperMode: Boolean,
) {
    fun findAccessGroups(client: VeoClientId): List<GroupRepresentation> =
        facade.perform {
            groups()
                .group(getClientGroup(client).id)
                .getSubGroups(0, Int.MAX_VALUE, false)
                .filter { AccessGroupSurrogateId.byGroupName(it.name) != null }
        }

    fun getAccessGroup(
        ref: AccessGroupSurrogateId,
        client: VeoClientId,
        notFoundExConstructor: (String) -> Throwable = ::ResourceNotFoundException,
    ): GroupRepresentation =
        facade.perform {
            groups().group(getClientGroup(client).id).getSubGroups(ref.groupName, true, 0, 1, false).firstOrNull()
                ?: throw notFoundExConstructor("Access group $ref not found")
        }

    fun getAccessGroups(
        surrogateIds: Collection<AccessGroupSurrogateId>,
        client: VeoClientId,
    ): List<GroupRepresentation> {
        val allGroups = findAccessGroups(client)
        return surrogateIds.map { surrogateId ->
            allGroups.find { it.name == surrogateId.groupName } ?: throw UnprocessableDtoException("Access group $surrogateId not found")
        }
    }

    fun createAccessGroup(
        attributes: Map<String, List<String>>,
        client: VeoClientId,
    ): AccessGroupSurrogateId =
        facade.performSynchronized(client) {
            val surrogateId = AccessGroupSurrogateId()
            log.info("Creating access group $surrogateId")
            groups()
                .group(getClientGroup(client).id)
                .subGroup(
                    GroupRepresentation().also {
                        it.name = surrogateId.groupName
                        it.attributes = attributes
                    },
                ).apply { if (status != 201) throw InternalError(readEntity(String::class.java)) }
            surrogateId
        }

    fun updateAccessGroup(
        surrogateId: AccessGroupSurrogateId,
        newAttributes: Map<String, List<String>>,
        client: VeoClientId,
    ): Unit =
        facade.performSynchronized(client) {
            log.info("Updating access group $surrogateId")
            groups().group(getAccessGroup(surrogateId, client).id).run {
                val rep = toRepresentation()
                rep.attributes = rep.attributes + newAttributes
                update(rep)
            }
        }

    fun deleteAccessGroup(
        surrogateId: AccessGroupSurrogateId,
        client: VeoClientId,
    ) = facade.performSynchronized(client) {
        getAccessGroup(surrogateId, client).apply {
            groups().group(id).remove()
        }
    }

    fun getAssignableGroup(group: AssignableGroup): GroupRepresentation =
        findGroup(group.groupName) ?: throw IllegalStateException("Assignable group $group not found")

    fun getClientGroup(
        client: VeoClientId,
        briefRepresentation: Boolean = false,
    ): GroupRepresentation =
        findGroup(client.groupName, briefRepresentation)
            ?: throw UnprocessableDtoException("Client ${client.clientId} not found")

    fun clientIsActive(veoClient: VeoClientId): Boolean =
        getClientGroup(veoClient).attributes[ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED] != listOf("true")

    fun createClient(
        client: VeoClientId,
        maxUnits: Int,
        maxUsers: Int,
    ) = facade.performSynchronized(client) {
        validateClientLimit()

        log.info("Creating veo client group ${client.groupName}")
        GroupRepresentation()
            .apply {
                name = client.groupName
                singleAttribute("maxUnits", maxUnits.toString())
                singleAttribute("maxUsers", maxUsers.toString())
            }.let { groups().add(it) }
            .run {
                if (!HttpStatusCode.valueOf(status).is2xxSuccessful) {
                    log.error { "Failed to create veo client group $client, unexpected status code $status" }
                    log.error { "Keycloak response: ${readEntity(String::class.java)}" }
                    throw InternalError()
                }
                log.info("Created veo client group $client")
            }
    }

    fun activateClient(veoClient: VeoClientId) =
        facade.performSynchronized(veoClient) {
            getClientGroup(veoClient)
                .apply { attributes.remove(ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED) }
                .also { groups().group(it.id).update(it) }
                .let { groups().group(it.id).members() }
                .forEach {
                    users().get(it.id).joinGroup(getGroup("veo-user").id)
                }
        }

    fun deactivateClient(veoClient: VeoClientId) =
        facade.performSynchronized(veoClient) {
            getClientGroup(veoClient)
                .apply { singleAttribute(ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED, "true") }
                .also { groups().group(it.id).update(it) }
                .let { groups().group(it.id).members() }
                .map { users().get(it.id) }
                .filter { userResource -> userResource.groups().any { it.name == "veo-user" } }
                .forEach {
                    it.leaveGroup(getGroup("veo-user").id)
                }
        }

    fun updateClient(
        client: VeoClientId,
        maxUnits: Int?,
        maxUsers: Int?,
    ) = facade.performSynchronized(client) {
        getClientGroup(client)
            .apply {
                maxUnits?.let { singleAttribute("maxUnits", it.toString()) }
                maxUsers?.let { singleAttribute("maxUsers", it.toString()) }
            }.let { groups().group(it.id).update(it) }
    }

    fun updateClient(
        client: VeoClientId,
        newRoles: Collection<String>,
    ) = facade.performSynchronized(client) {
        getClientGroup(client)
            .let {
                val allRoles by lazy { roles().list() }
                val currentRoleNames = it.realmRoles
                val rolesToRemove =
                    currentRoleNames
                        .filter { !newRoles.contains(it) }
                        .map { roleName -> allRoles.find { it.name == roleName } }
                val rolesToAdd =
                    newRoles
                        .filter { !currentRoleNames.contains(it) }
                        .map { roleName -> allRoles.find { it.name == roleName } }
                groups()
                    .group(it.id)
                    .roles()
                    .realmLevel()
                    .apply {
                        if (rolesToAdd.isNotEmpty()) {
                            add(rolesToAdd)
                        }
                        if (rolesToRemove.isNotEmpty()) {
                            remove(rolesToRemove)
                        }
                    }
            }
    }

    fun deleteClient(client: VeoClientId) =
        facade.performSynchronized(client) {
            log.info("Deleting veo client group ${client.groupName}")
            groups().group(getClientGroup(client).id).run {
                members().forEach {
                    users().delete(it.id)
                }
                remove()
            }
        }

    private fun validateClientLimit() {
        if (isDeveloperMode) return
        val clientsMax = licenseService.getLicensedTotalClients()
        val currentClientCount = getNumberOfActiveClients()
        if (currentClientCount >= clientsMax) {
            throw ExceedingMaxClientsException(clientsMax)
        }
    }

    fun getNumberOfActiveClients(): Int =
        facade.perform {
            groups()
                .groups(CLIENT_GROUP_PREFIX, 0, Int.MAX_VALUE, false)
                .count { group ->
                    group.attributes?.get(ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED)?.firstOrNull() != "true"
                }
        }

    private fun getGroup(groupName: String) = findGroup(groupName, true) ?: throw IllegalStateException("Group '$groupName' not found")

    private fun findGroup(
        groupName: String,
        briefRepresentation: Boolean = false,
    ): GroupRepresentation? =
        facade.perform {
            groups()
                .groups(groupName, 0, 1, briefRepresentation)
                .firstOrNull()
                ?.let { listOf(it) + it.subGroups }
                ?.firstOrNull { it.name == groupName }
        }

    fun removeUnitRights(
        unit: UnitId,
        client: VeoClientId,
    ) {
        findAccessGroups(client).forEach { group ->
            UnitAccessRights
                .byAttributes(group.attributes)
                .takeIf { unit in it.value }
                ?.withoutUnit(unit)
                ?.toAttributes()
                ?.also {
                    updateAccessGroup(
                        AccessGroupSurrogateId.byGroupName(group.name)!!,
                        it,
                        client,
                    )
                }
        }
    }

    fun setGlobalWriteAccessEnabled(flag: Boolean) {
        getAssignableGroup(AssignableGroup.VEO_WRITE_ACCESS).also { group ->
            val roleName = "veo-write"
            val existingRoles = group.realmRoles.orEmpty()
            val hasRole = roleName in existingRoles
            if (flag != hasRole) {
                facade.perform {
                    val role = roles().get(roleName).toRepresentation()
                    val roleScopeResource = groups().group(group.id).roles().realmLevel()
                    if (flag) {
                        roleScopeResource.add(listOf(role))
                    } else {
                        roleScopeResource.remove(listOf(role))
                    }
                }
            }
        }
    }

    fun getNumberOfClients(): Long = facade.perform { groups().count(CLIENT_GROUP_PREFIX)["count"]!! }
}
