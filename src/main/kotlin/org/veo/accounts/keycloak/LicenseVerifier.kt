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

import org.springframework.stereotype.Component
import org.veo.accounts.License
import org.veo.accounts.systemmessages.LicenseMessage
import org.veo.accounts.systemmessages.MessageLevel
import org.veo.accounts.systemmessages.VeoApiService
import java.time.Duration
import java.time.Instant

private val LICENSE_INITIAL =
    LicenseMessage(
        "In Ihrem System ist keine Lizenz hinterlegt. Bitte kaufen Sie eine Lizenz.",
        "There is no license available in your system. Please purchase a license.",
        MessageLevel.URGENT,
    )
private val LICENSE_EXPIRED =
    LicenseMessage(
        "Ihre Lizenz ist abgelaufen. Bitte kaufen Sie eine neue Lizenz.",
        "Your license is expired. Please purchase a new one.",
        MessageLevel.URGENT,
    )

@Component
class LicenseVerifier(
    private val groupService: GroupService,
    private val veoApiService: VeoApiService,
    private val accountService: AccountService,
) {
    fun checkLicense(license: License?) {
        if (license == null) {
            veoApiService.setLicenseMessages(setOf(LICENSE_INITIAL))
            groupService.setGlobalWriteAccessEnabled(false)
        } else {
            var writeAccess = true
            val messages = HashSet<LicenseMessage>()
            val numberOfClients = groupService.getNumberOfClients()
            val numberOfUsers = accountService.countEnabledUsers()
            val numberOfUnits = veoApiService.getNumberOfUnits()

            if (numberOfClients > license.totalClients) {
                writeAccess = false
                messages.add(
                    LicenseMessage(
                        "Ihre Lizenz ist auf ${license.totalClients} Clients beschränkt, es wurden aber $numberOfClients angelegt.",
                        "Your license is restricted to ${license.totalClients} clients, but $numberOfClients were created.",
                        MessageLevel.URGENT,
                    ),
                )
            }
            if (numberOfUsers > license.totalUsers) {
                writeAccess = false
                messages.add(
                    LicenseMessage(
                        "Ihre Lizenz ist auf ${license.totalUsers} Benutzer beschränkt, es wurden aber $numberOfUsers angelegt.",
                        "Your license is restricted to ${license.totalUsers} users, but $numberOfUsers were created.",
                        MessageLevel.URGENT,
                    ),
                )
            }
            if (numberOfUnits > license.totalUnits) {
                writeAccess = false
                messages.add(
                    LicenseMessage(
                        "Ihre Lizenz ist auf ${license.totalUnits} Units beschränkt, es wurden aber $numberOfUnits angelegt.",
                        "Your license is restricted to ${license.totalUnits} units, but $numberOfUnits were created.",
                        MessageLevel.URGENT,
                    ),
                )
            }
            val licenseValidRemainingDays = Duration.between(Instant.now(), license.validUntil).toDays()
            if (licenseValidRemainingDays < 0) {
                // license expired
                writeAccess = false
                messages.add(LICENSE_EXPIRED)
            } else if (licenseValidRemainingDays < 7) {
                // license expiring soon
                messages.add(
                    LicenseMessage(
                        "Ihre Lizenz läuft in $licenseValidRemainingDays Tagen ab. Bitte kaufen Sie bald eine neue Lizenz.",
                        "Your license expires in $licenseValidRemainingDays days. Please purchase a new one soon.",
                        MessageLevel.WARNING,
                    ),
                )
            }
            groupService.setGlobalWriteAccessEnabled(writeAccess)
            veoApiService.setLicenseMessages(messages)
        }
    }
}
