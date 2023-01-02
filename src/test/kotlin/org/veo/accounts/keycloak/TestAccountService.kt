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
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.GroupRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.veo.accounts.Role
import org.veo.accounts.auth.VeoClient
import java.util.UUID
import java.util.UUID.randomUUID
import javax.ws.rs.NotFoundException

@Component
@Profile("resttest")
class TestAccountService(
    @Value("\${veo.resttest.clientId}")
    private val clientId: String,
) {
    @Autowired
    private lateinit var facade: KeycloakFacade

    val testPassword = randomUUID().toString()
    private val createdAccountIds = mutableListOf<String>()
    private val createdGroupIds = mutableMapOf<VeoClient, UUID>()

    fun createManager(group: VeoClient, roles: List<Role>, usernamePrefix: String): String = facade.perform {
        UserRepresentation()
            .apply {
                username = "$usernamePrefix-account-${randomUUID()}"
                isEnabled = true
                isEmailVerified = true
                groups = listOf(group.path)
            }
            .let { users().create(it) }
            .getHeaderString("Location")
            .substringAfterLast('/')
            .also { assignRoles(it, roles) }
            .also { assignTestPassword(it) }
            .also(createdAccountIds::add)
    }

    fun createVeoClientGroup(maxUsers: Int): VeoClient = facade.perform {
        VeoClient(randomUUID())
            .also { createdGroupIds[it] = createVeoClientGroup(it, maxUsers) }
    }

    fun updateMaxUsers(client: VeoClient, maxUsers: Int) = facade.perform {
        groups()
            .group(createdGroupIds[client].toString())
            .run {
                update(
                    toRepresentation().also {
                        it.attributes["maxUsers"] = listOf(maxUsers.toString())
                    },
                )
            }
    }

    fun accountExists(accountId: String): Boolean = facade.perform {
        try {
            users().get(accountId).toRepresentation()
            true
        } catch (_: NotFoundException) {
            false
        }
    }

    fun cleanup() = facade.perform {
        createdAccountIds
            .onEach { tryDeleteAccount(it) }
            .clear()
        createdGroupIds
            .values
            .onEach { tryDeleteGroup(it.toString()) }
            .clear()
    }

    fun getUsername(accountId: String): String = facade.perform {
        users().get(accountId)
            .toRepresentation()
            .username
    }

    private fun RealmResource.createVeoClientGroup(client: VeoClient, maxUsers: Int): UUID = GroupRepresentation()
        .apply {
            name = client.groupName
            attributes = mapOf("maxUsers" to listOf(maxUsers.toString()))
        }
        .let { groups().add(it) }
        .let(facade::parseResourceId)
        .let(UUID::fromString)

    private fun RealmResource.assignTestPassword(accountId: String) = users()
        .get(accountId)
        .resetPassword(
            CredentialRepresentation().apply {
                isTemporary = false
                type = "password"
                value = testPassword
            },
        )

    private fun RealmResource.assignRoles(
        accountId: String,
        roles: List<Role>,
    ) = users().get(accountId)
        .roles()
        .clientLevel(clientId)
        .apply { add(listAvailable().filter { roles.map(Role::roleName).contains(it.name) }) }
}
