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

import mu.KotlinLogging.logger
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.openssl.PEMParser
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.veo.accounts.AdminController
import org.veo.accounts.License
import org.veo.accounts.exceptions.ExceedingMaxClientsException
import org.veo.accounts.exceptions.InvalidLicenseException
import org.veo.accounts.exceptions.MissingLicenseException
import tools.jackson.databind.ObjectMapper
import java.security.cert.CertificateFactory
import java.time.Instant
import kotlin.collections.plus
import kotlin.getValue

private val log = logger {}

@Component
class LicenseService(
    private val keycloakFacade: KeycloakFacade,
    private val objectMapper: ObjectMapper,
    @Lazy private val licenseVerifier: LicenseVerifier,
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
        licenseVerifier.checkLicense(license)
    }

    fun findInstalledLicense(): License? =
        keycloakFacade.perform {
            val licenseString = toRepresentation().attributes["veo-license"]
            if (licenseString == null) null else parseLicense(licenseString)
        }

    fun getInstalledLicense(): License = findInstalledLicense() ?: throw MissingLicenseException()

    fun getLicensedTotalUsers(): Int = getInstalledLicense().totalUsers

    fun getLicensedTotalClients(): Int = getInstalledLicense().totalClients

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
}
