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
import org.veo.accounts.dtos.VeoClientId
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

    fun createManager(group: VeoClientId, roles: List<Role>, usernamePrefix: String): String = facade.perform {
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
    }

    fun updateMaxUsers(client: VeoClientId, maxUsers: Int) = facade.perform {
        findGroup(client.groupName)!!
            .singleAttribute("maxUsers", maxUsers.toString())
            .let { groups().group(it.id).update(it) }
    }

    fun findAccount(accountId: String): UserRepresentation? = facade.perform {
        try {
            users().get(accountId).toRepresentation()
        } catch (_: NotFoundException) {
            null
        }
    }

    fun findGroup(groupName: String): GroupRepresentation? = facade.perform {
        groups().groups(groupName, true, 0, 1, false).firstOrNull()
    }

    fun accountInGroup(accountId: String, groupName: String): Boolean = facade.perform {
        users().get(accountId).groups().any { it.name == groupName }
    }

    fun getUsername(accountId: String): String = facade.perform {
        users().get(accountId)
            .toRepresentation()
            .username
    }

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

    fun getAccountGroups(accountId: String): List<String> = facade.perform {
        users().get(accountId).groups().map { it.name }
    }
}
