/*
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

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.keycloak.admin.client.resource.RealmResource
import org.veo.accounts.auth.AuthenticatedAccount
import org.veo.accounts.createAccountDto
import org.veo.accounts.createInitialAccountDto
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.exceptions.MissingLicenseException

class AccountServiceTest {
    private val keycloak =
        mockk<KeycloakFacade> {
            val realmResource = mockk<RealmResource> {}
            every { performSynchronized<Any?>(any<AuthenticatedAccount>(), any()) } answers {
                val block = arg<RealmResource.() -> Any>(1)
                realmResource.block()
            }
            every { performSynchronized<Any?>(any<VeoClientId>(), any()) } answers {
                val block = arg<RealmResource.() -> Any>(1)
                realmResource.block()
            }
        }

    private val groupService = mockk<GroupService>()
    private val licenseService = mockk<LicenseService>()
    private val sut = AccountService("veo-user", keycloak, groupService, licenseService, false, "test.example", "my-keycloak-client", false)

    @Test
    fun `account creation fails without license`() {
        every { licenseService.getLicensedTotalUsers() } throws MissingLicenseException()

        shouldThrow<MissingLicenseException> {
            sut.createAccount(createAccountDto(), mockk())
        }
        shouldThrow<MissingLicenseException> {
            sut.createInitialAccount(createInitialAccountDto())
        }
    }
}
