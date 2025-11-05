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

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging.logger
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.openssl.PEMParser
import org.springframework.stereotype.Component
import org.veo.accounts.AdminController
import org.veo.accounts.License
import org.veo.accounts.exceptions.InvalidLicenseException
import org.veo.accounts.exceptions.MissingLicenseException
import org.veo.accounts.systemmessages.LicenseMessage
import org.veo.accounts.systemmessages.MessageLevel
import org.veo.accounts.systemmessages.VeoApiService
import java.security.cert.CertificateFactory
import java.time.Duration
import java.time.Instant
import kotlin.collections.plus
import kotlin.getValue

private val log = logger {}

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
class LicenseService(
    private val keycloakFacade: KeycloakFacade,
    private val objectMapper: ObjectMapper,
    private val groupService: GroupService,
    private val veoApiService: VeoApiService,
) {
    private val verifier by lazy {
        CertificateFactory
            .getInstance("X.509")
            .generateCertificate(AdminController::class.java.getResourceAsStream("/cert.pem"))
            .publicKey
            .let { JcaSimpleSignerInfoVerifierBuilder().build(it) }
    }

    fun saveLicense(licenseString: String) {
        val license = parseLicense(licenseString)

        if (license.validUntil.isBefore(Instant.now())) {
            throw InvalidLicenseException("License expired.")
        }

        keycloakFacade.perform {
            toRepresentation().let {
                it.attributes = it.attributesOrEmpty + mapOf("veo-license" to licenseString)
                update(it)
            }
            clientScopes().findAll().first { it.name == "veo-license" }.also { licenseScope ->
                licenseScope.protocolMappers.first { it.name == "total units" }.apply {
                    config["claim.value"] = license.totalUnits.toString()
                    clientScopes().get(licenseScope.id).protocolMappers.update(id, this)
                }
            }
        }
        checkLicense(license)
    }

    fun findInstalledLicense(): License? =
        keycloakFacade.perform {
            val licenseString = toRepresentation().attributes["veo-license"]
            if (licenseString == null) null else parseLicense(licenseString)
        }

    fun getInstalledLicense(): License = findInstalledLicense() ?: throw MissingLicenseException()

    fun getLicensedTotalUsers(): Int = getInstalledLicense().totalUsers.toInt()

    private fun parseLicense(licenseString: String): License {
        val signers: Collection<SignerInformation>
        val license =
            try {
                val contentInfo =
                    PEMParser(licenseString.reader()).use {
                        it.readObject() as ContentInfo
                    }
                val signedData = CMSSignedData(contentInfo.encoded)
                val signedContent = signedData.signedContent as CMSProcessableByteArray

                signers = signedData.signerInfos.signers
                objectMapper.readValue(signedContent.inputStream, License::class.java)
            } catch (e: Exception) {
                log.error("Failed to read license", e)
                throw InvalidLicenseException("Request body does not represent a valid license.")
            }

        if (signers.isEmpty()) {
            throw InvalidLicenseException("Signature missing.")
        }

        if (!signers.all { it.verify(verifier) }) {
            throw InvalidLicenseException("Invalid signature.")
        }

        return license
    }

    fun checkInstalledLicense() {
        checkLicense(findInstalledLicense())
    }

    fun checkLicense(license: License?) {
        if (license == null) {
            veoApiService.setLicenseMessages(setOf(LICENSE_INITIAL))
            groupService.setGlobalWriteAccessEnabled(false)
        } else {
            val licenseValidRemainingDays = Duration.between(Instant.now(), license.validUntil).toDays()
            if (licenseValidRemainingDays < 0) {
                // license expired
                veoApiService.setLicenseMessages(setOf(LICENSE_EXPIRED))
                groupService.setGlobalWriteAccessEnabled(false)
            } else if (licenseValidRemainingDays < 7) {
                // license expiring soon
                veoApiService.setLicenseMessages(
                    setOf(
                        LicenseMessage(
                            "Ihre Lizenz läuft in $licenseValidRemainingDays Tagen ab. Bitte kaufen Sie bald eine neue Lizenz.",
                            "Your license expires in $licenseValidRemainingDays days. Please purchase a new one soon.",
                            MessageLevel.WARNING,
                        ),
                    ),
                )
                groupService.setGlobalWriteAccessEnabled(true)
            } else {
                veoApiService.setLicenseMessages(setOf())
                groupService.setGlobalWriteAccessEnabled(true)
            }
        }
    }
}
