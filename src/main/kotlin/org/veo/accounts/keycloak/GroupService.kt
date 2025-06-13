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
import org.keycloak.representations.idm.GroupRepresentation
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.veo.accounts.AssignableGroup
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.exceptions.UnprocessableDtoException

private const val ATTRIBUTE_VEO_CLIENT_GROUP_DEACTIVATED = "veo-accounts.deactivated"
private val log = logger {}

@Component
class GroupService(
    private val facade: KeycloakFacade,
) {
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
}
