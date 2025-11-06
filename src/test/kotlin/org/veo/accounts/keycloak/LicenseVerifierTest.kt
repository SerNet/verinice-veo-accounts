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
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.veo.accounts.License
import org.veo.accounts.systemmessages.LicenseMessage
import org.veo.accounts.systemmessages.MessageLevel
import org.veo.accounts.systemmessages.VeoApiService
import java.time.Instant
import java.time.temporal.ChronoUnit

class LicenseVerifierTest {
    private val groupService = mockk<GroupService>()
    private val veoApiService = mockk<VeoApiService>()
    private val accountService = mockk<AccountService>()
    private val sut = LicenseVerifier(groupService, veoApiService, accountService)

    @Test
    fun `write access is removed if no license is present`() {
        every { accountService.countEnabledUsers() } returns 1
        every { groupService.getNumberOfClients() } returns 1
        every { groupService.setGlobalWriteAccessEnabled(false) } just Runs
        every { veoApiService.getNumberOfUnits() } returns 1
        every { veoApiService.setLicenseMessages(any()) } just Runs

        sut.checkLicense(null)

        verify {
            groupService.setGlobalWriteAccessEnabled(false)
            veoApiService.setLicenseMessages(
                setOf(
                    LicenseMessage(
                        "In Ihrem System ist keine Lizenz hinterlegt. Bitte kaufen Sie eine Lizenz.",
                        "There is no license available in your system. Please purchase a license.",
                        MessageLevel.URGENT,
                    ),
                ),
            )
        }
    }

    @Test
    fun `write access is granted if client, units, and users stay within license boundaries`() {
        val license = License("1", 1, 1, 1, Instant.parse("3000-01-01T00:00:00Z"))
        every { accountService.countEnabledUsers() } returns 1
        every { groupService.getNumberOfClients() } returns 1
        every { groupService.setGlobalWriteAccessEnabled(true) } just Runs
        every { veoApiService.getNumberOfUnits() } returns 1
        every { veoApiService.setLicenseMessages(any()) } just Runs

        sut.checkLicense(license)

        verify {
            groupService.setGlobalWriteAccessEnabled(true)
            veoApiService.setLicenseMessages(setOf())
        }
    }

    @Test
    fun `write access is removed if license expires soon`() {
        val license = License("1", 1, 1, 1, Instant.now().plus(3, ChronoUnit.DAYS))
        every { accountService.countEnabledUsers() } returns 0
        every { groupService.getNumberOfClients() } returns 0
        every { groupService.setGlobalWriteAccessEnabled(true) } just Runs
        every { veoApiService.getNumberOfUnits() } returns 0
        every { veoApiService.setLicenseMessages(any()) } just Runs

        sut.checkLicense(license)

        verify {
            groupService.setGlobalWriteAccessEnabled(true)
            veoApiService.setLicenseMessages(
                setOf(
                    LicenseMessage(
                        "Ihre Lizenz läuft in 2 Tagen ab. Bitte kaufen Sie bald eine neue Lizenz.",
                        "Your license expires in 2 days. Please purchase a new one soon.",
                        MessageLevel.WARNING,
                    ),
                ),
            )
        }
    }

    @Test
    fun `write access is removed if license is expired`() {
        val license = License("1", 1, 1, 1, Instant.parse("2000-01-01T00:00:00Z"))
        every { accountService.countEnabledUsers() } returns 0
        every { groupService.getNumberOfClients() } returns 0
        every { groupService.setGlobalWriteAccessEnabled(false) } just Runs
        every { veoApiService.getNumberOfUnits() } returns 0
        every { veoApiService.setLicenseMessages(any()) } just Runs

        sut.checkLicense(license)

        verify {
            groupService.setGlobalWriteAccessEnabled(false)
            veoApiService.setLicenseMessages(
                setOf(
                    LicenseMessage(
                        "Ihre Lizenz ist abgelaufen. Bitte kaufen Sie eine neue Lizenz.",
                        "Your license is expired. Please purchase a new one.",
                        MessageLevel.URGENT,
                    ),
                ),
            )
        }
    }

    @Test
    fun `write access is removed if number of units exceeds license`() {
        val license = License("1", 0, 0, 0, Instant.parse("3000-01-01T00:00:00Z"))
        every { accountService.countEnabledUsers() } returns 0
        every { groupService.getNumberOfClients() } returns 0
        every { groupService.setGlobalWriteAccessEnabled(false) } just Runs
        every { veoApiService.getNumberOfUnits() } returns 1
        every { veoApiService.setLicenseMessages(any()) } just Runs

        sut.checkLicense(license)

        verify {
            groupService.setGlobalWriteAccessEnabled(false)
            veoApiService.setLicenseMessages(
                setOf(
                    LicenseMessage(
                        "Ihre Lizenz ist auf 0 Units beschränkt, es wurden aber 1 angelegt.",
                        "Your license is restricted to 0 units, but 1 were created.",
                        MessageLevel.URGENT,
                    ),
                ),
            )
        }
    }

    @Test
    fun `write access is removed if number of users exceeds license`() {
        val license = License("1", 0, 0, 0, Instant.parse("3000-01-01T00:00:00Z"))
        every { accountService.countEnabledUsers() } returns 1
        every { groupService.getNumberOfClients() } returns 0
        every { groupService.setGlobalWriteAccessEnabled(false) } just Runs
        every { veoApiService.getNumberOfUnits() } returns 0
        every { veoApiService.setLicenseMessages(any()) } just Runs

        sut.checkLicense(license)

        verify {
            groupService.setGlobalWriteAccessEnabled(false)
            veoApiService.setLicenseMessages(
                setOf(
                    LicenseMessage(
                        "Ihre Lizenz ist auf 0 Benutzer beschränkt, es wurden aber 1 angelegt.",
                        "Your license is restricted to 0 users, but 1 were created.",
                        MessageLevel.URGENT,
                    ),
                ),
            )
        }
    }

    @Test
    fun `write access is removed if number of clients exceeds license`() {
        val license = License("1", 0, 0, 0, Instant.parse("3000-01-01T00:00:00Z"))
        every { accountService.countEnabledUsers() } returns 0
        every { groupService.getNumberOfClients() } returns 1
        every { groupService.setGlobalWriteAccessEnabled(false) } just Runs
        every { veoApiService.getNumberOfUnits() } returns 0
        every { veoApiService.setLicenseMessages(any()) } just Runs

        sut.checkLicense(license)

        verify {
            groupService.setGlobalWriteAccessEnabled(false)
            veoApiService.setLicenseMessages(
                setOf(
                    LicenseMessage(
                        "Ihre Lizenz ist auf 0 Clients beschränkt, es wurden aber 1 angelegt.",
                        "Your license is restricted to 0 clients, but 1 were created.",
                        MessageLevel.URGENT,
                    ),
                ),
            )
        }
    }
}
