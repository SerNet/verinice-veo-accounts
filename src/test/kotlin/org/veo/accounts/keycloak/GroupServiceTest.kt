/**
 * verinice.veo accounts
 * Copyright (C) 2025  Jochen Kemnade
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

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.keycloak.admin.client.resource.GroupResource
import org.keycloak.admin.client.resource.GroupsResource
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.admin.client.resource.RoleMappingResource
import org.keycloak.admin.client.resource.RoleResource
import org.keycloak.admin.client.resource.RoleScopeResource
import org.keycloak.admin.client.resource.RolesResource
import org.keycloak.representations.idm.GroupRepresentation
import org.keycloak.representations.idm.RoleRepresentation
import org.veo.accounts.AssignableGroup
import org.veo.accounts.dtos.AccessGroupSurrogateId
import org.veo.accounts.dtos.UnitId
import org.veo.accounts.dtos.VeoClientId
import java.util.UUID

class GroupServiceTest {
    private val facade = mockk<KeycloakFacade>()
    private val licenseService = mockk<LicenseService>()

    private val sut = spyk(GroupService(facade, licenseService, false))

    @Test
    fun `removes unit from access groups`() {
        val clientId = VeoClientId(UUID.fromString("cc12aad0-b9fb-46a0-9beb-489ed40ebb24"))
        val unitId = "e9aba324-9ba1-4b2f-9b5b-1d67b3394d08"
        val otherUnitId = "d42403bc-5b9c-4b35-9f62-7c24251ea2ed"
        val group1Name = "access_group_f66c03a2-efec-48d4-b330-b6784e1b0c5a"
        val group2Name = "access_group_1c27d400-01ae-4539-a985-0d55a993fef3"
        val accessGroup1SurrogateId = AccessGroupSurrogateId.byGroupName(group1Name)!!
        val accessGroup2SurrogateId = AccessGroupSurrogateId.byGroupName(group2Name)!!

        val group1 =
            mockk<GroupRepresentation> {
                every { name } returns group1Name

                every { attributes } returns
                    mapOf(
                        "unitReadAccess" to
                            listOf(
                                unitId,
                            ),
                    )
            }
        val group2 =
            mockk<GroupRepresentation> {
                every { name } returns group2Name

                every { attributes } returns
                    mapOf(
                        "unitReadAccess" to
                            listOf(
                                otherUnitId,
                            ),
                    )
            }

        val expectedNewAttributes =
            mapOf(
                "unitReadAccess" to emptyList<String>(),
                "unitWriteAccess" to emptyList(),
            )

        every { sut.findAccessGroups(clientId) } returns listOf(group1, group2)
        every { sut.updateAccessGroup(any(), any(), any()) } just Runs

        // when unit rights are removed
        sut.removeUnitRights(
            UnitId(unitId),
            clientId,
        )

        // then permissions for the unit are removed from all access groups
        verify {
            sut.updateAccessGroup(
                accessGroup1SurrogateId,
                expectedNewAttributes,
                clientId,
            )
        }
        verify(exactly = 0) { sut.updateAccessGroup(accessGroup2SurrogateId, any(), any()) }
    }

    @Test
    fun `removes global write access`() {
        val veoWriteAccessId = UUID.randomUUID().toString()

        val veoWriteAccess =
            mockk<GroupRepresentation> {
                every { name } returns AssignableGroup.VEO_WRITE_ACCESS.groupName
                every { id } returns veoWriteAccessId
                every { realmRoles } returns listOf("veo-write")
                every { realmRoles = any() } just Runs
            }

        val veoWrite = mockk<RoleRepresentation>()

        val roleScopeResource =
            mockk<RoleScopeResource> {
                every { remove(listOf(veoWrite)) } just Runs
            }

        every { sut.getAssignableGroup(AssignableGroup.VEO_WRITE_ACCESS) } returns veoWriteAccess

        val realmResource =
            mockk<RealmResource> {
                every { groups() } returns
                    mockk<GroupsResource> {
                        every { group(veoWriteAccessId) } returns
                            mockk<GroupResource> {
                                every { roles() } returns
                                    mockk<RoleMappingResource> {
                                        every { realmLevel() } returns roleScopeResource
                                    }
                            }
                    }
                every { roles() } returns
                    mockk<RolesResource> mock@{
                        every { this@mock.get("veo-write") } returns
                            mockk<RoleResource> {
                                every { toRepresentation() } returns veoWrite
                            }
                    }
            }
        every { facade.perform<Unit>(any()) } answers {
            val block = arg<RealmResource.() -> Any>(0)
            realmResource.block()
        }

        sut.setGlobalWriteAccessEnabled(false)

        verify {
            roleScopeResource.remove(listOf(veoWrite))
        }
    }

    @Test
    fun `adds global write access`() {
        val veoWriteAccessId = UUID.randomUUID().toString()

        val veoWriteAccess =
            mockk<GroupRepresentation> {
                every { name } returns AssignableGroup.VEO_WRITE_ACCESS.groupName
                every { id } returns veoWriteAccessId
                every { realmRoles } returns listOf()
                every { realmRoles = any() } just Runs
            }

        val veoWrite = mockk<RoleRepresentation>()

        val roleScopeResource =
            mockk<RoleScopeResource> {
                every { add(listOf(veoWrite)) } just Runs
            }

        every { sut.getAssignableGroup(AssignableGroup.VEO_WRITE_ACCESS) } returns veoWriteAccess

        val realmResource =
            mockk<RealmResource> {
                every { groups() } returns
                    mockk<GroupsResource> {
                        every { group(veoWriteAccessId) } returns
                            mockk<GroupResource> {
                                every<RoleMappingResource> { roles() } returns
                                    mockk<RoleMappingResource> {
                                        every<RoleScopeResource> { realmLevel() } returns roleScopeResource
                                    }
                            }
                    }
                every { roles() } returns
                    mockk<RolesResource> {
                        every { roles() } returns
                            mockk<RolesResource> mock@{
                                every { this@mock.get("veo-write") } returns
                                    mockk<RoleResource> {
                                        every { toRepresentation() } returns veoWrite
                                    }
                            }
                    }
            }

        every { facade.perform<Unit>(any()) } answers {
            val block = arg<RealmResource.() -> Any>(0)
            realmResource.block()
        }

        sut.setGlobalWriteAccessEnabled(true)

        verify {
            roleScopeResource.add(listOf(veoWrite))
        }
    }
}
