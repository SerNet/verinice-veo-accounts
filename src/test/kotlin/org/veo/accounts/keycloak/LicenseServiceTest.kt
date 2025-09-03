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

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.RealmRepresentation
import org.veo.accounts.exceptions.InvalidLicenseException

private val om = jacksonObjectMapper().registerModule(JavaTimeModule())

class LicenseServiceTest {
    private val facade = mockk<KeycloakFacade>()

    private val sut = LicenseService(facade, om)

    @Test
    fun `license can be saved`() {
        val licenseString =
            """
            -----BEGIN PKCS7-----
            MIIGHgYJKoZIhvcNAQcCoIIGDzCCBgsCAQExDzANBglghkgBZQMEAgEFADCBoQYJ
            KoZIhvcNAQcBoIGTBIGQQ29udGVudC1UeXBlOiB0ZXh0L3BsYWluDQoNCnsiY3Vz
            dG9tZXJObyI6IjEyMzQiLCJ0b3RhbENsaWVudHMiOjAsInRvdGFsVW5pdHMiOjAs
            InRvdGFsVXNlcnMiOjAsInZhbGlkVW50aWwiOiIzMDAwLTAxLTAxVDAwOjAwOjAw
            LjAwMDAwMDAwMFoifQ0KoIIDDTCCAwkwggHxoAMCAQICFD9tVPvLPIUODW0wCUWD
            RyQv6Gm5MA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNVBAMMCVRlc3QgQ2VydDAeFw0y
            NTA5MDQwODMwMDNaFw0yNTA5MDUwODMwMDNaMBQxEjAQBgNVBAMMCVRlc3QgQ2Vy
            dDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMa8bs20RNMPvd2HToJF
            aDJ0ZhEE0116/aPzkTE6hyn4guIoaiEcbZOOQeGgdAMluIT1uE92huWI0E33XmBh
            egr/8AqJ+gUQIOARzlLVJLCiG6SOefBP+OYvbJmPYpUNujU9Ul2soqNBpwe19zsM
            V2DHMDZggPeQzfwNuzmsC09o3Rn66Hq/KbAmmdjpqH0e7s+uVYa0sc1fWBp21/TR
            /quMtkxG6c5aEgTAI1B4PvMace7uvr5tljyCVIHCViGpMdPjCHxQQlAScivkrwHu
            jUfjZ5PyqfA4uVxYp/2gb4rCTQ8y84g8BEOgdWD96iVM6MU1OC+GxvQqRmkrqBJW
            STECAwEAAaNTMFEwHQYDVR0OBBYEFL5aET7chEXiKAvIrwEMYMWFW9ZMMB8GA1Ud
            IwQYMBaAFL5aET7chEXiKAvIrwEMYMWFW9ZMMA8GA1UdEwEB/wQFMAMBAf8wDQYJ
            KoZIhvcNAQELBQADggEBAFiajX+vO/4q1ADK9xkw3WEYDxX92SxB69d3I1OlE7U8
            +Rd6NdCoMyUF7ZRZlnvpNJfBzpR+svWOGrmxNPn8jAkXGXdmYx35PGWRsmb4qMnt
            TbUv0FTgEu1NUY3tqMoTNltW+LYUw1Zf7jP98kFGbtztc10zyZhcUNOGJVS77W6v
            wVzMF6H9OaxM5W4Uyk+A4qV7rh/AKLdxV5dkzoaEsijCIKyGwDw8XMbtuIrMrcOG
            JvvtddDpG5aBqaRCU74oHth9rdhMb+dh9eJN2OJxAsVnUNqwLVq2ZloU6mGyAjT3
            5C+tZZBUAVTfTrazGr0DB3aXA67BRr0XBKqP0VYxDgkxggI+MIICOgIBATAsMBQx
            EjAQBgNVBAMMCVRlc3QgQ2VydAIUP21U+8s8hQ4NbTAJRYNHJC/oabkwDQYJYIZI
            AWUDBAIBBQCggeQwGAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAcBgkqhkiG9w0B
            CQUxDxcNMjUwOTA0MTA0NDA4WjAvBgkqhkiG9w0BCQQxIgQgHjF7Iwi+otOZlONi
            Q0h2Sa6R31ReVJRENbFxnCKcF00weQYJKoZIhvcNAQkPMWwwajALBglghkgBZQME
            ASowCwYJYIZIAWUDBAEWMAsGCWCGSAFlAwQBAjAKBggqhkiG9w0DBzAOBggqhkiG
            9w0DAgICAIAwDQYIKoZIhvcNAwICAUAwBwYFKw4DAgcwDQYIKoZIhvcNAwICASgw
            DQYJKoZIhvcNAQEBBQAEggEAni7xKsuLKHkGty2//ZU4xtacW0QJuCs3NZGwKhG8
            SmsDPnCQIk46uO8hv8Hlw3+bnh2DcdIRRXVsnTz0evw8zDFdOvFNldNK6z5Fzdye
            roZgLIOEM1I0MgYOWYtmGT+t2rwzPcggrCdA0TXSNvittv8CmneUrPlpmW/+z7HK
            G7aZzuGdaMjtBJ9SxwADfei/2DkfW70nL6+ZHDiwf944wxSzCilZGNaPQn78mox/
            /ns1p6xs1SNcjqy/yj6avdFRwytuo0CCdmatdpqjH5TW7KN2BDz2x+pLi3XWaz4W
            PLcq14L5YKQ2XREwR6Z1WrXoLliRHAvFzvqTO70eDf7ppg==
            -----END PKCS7-----
            """.trimIndent()
        val realmSlot = slot<RealmRepresentation>()

        val realmResource =
            mockk<RealmResource> {
                every { update(capture(realmSlot)) } just Runs
                every { toRepresentation() } returns
                    RealmRepresentation().apply {
                        attributes = emptyMap()
                    }
            }

        every { facade.perform<Unit>(any()) } answers {
            val block = arg<RealmResource.() -> Any>(0)
            realmResource.block()
        }
        sut.saveLicense(licenseString)

        verify { realmResource.update(any()) }
        assert(realmSlot.captured.attributes["veo-license"] == licenseString)
    }

    @Test
    fun `file without signature is rejected`() {
        val exception =
            shouldThrow<InvalidLicenseException> {
                sut.saveLicense(
                    """
                    -----BEGIN PKCS7-----
                    MIIGUwYJKoZIhvcNAQcCoIIGRDCCBkACAQExADCCBjUGCSqGSIb3DQEHAaCCBiYE
                    ggYiMIIGHgYJKoZIhvcNAQcCoIIGDzCCBgsCAQExDzANBglghkgBZQMEAgEFADCB
                    oQYJKoZIhvcNAQcBoIGTBIGQQ29udGVudC1UeXBlOiB0ZXh0L3BsYWluDQoNCnsi
                    Y3VzdG9tZXJObyI6IjEyMzQiLCJ0b3RhbENsaWVudHMiOjAsInRvdGFsVW5pdHMi
                    OjAsInRvdGFsVXNlcnMiOjAsInZhbGlkVW50aWwiOiIzMDAwLTAxLTAxVDAwOjAw
                    OjAwLjAwMDAwMDAwMFoifQ0KoIIDDTCCAwkwggHxoAMCAQICFD9tVPvLPIUODW0w
                    CUWDRyQv6Gm5MA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNVBAMMCVRlc3QgQ2VydDAe
                    Fw0yNTA5MDQwODMwMDNaFw0yNTA5MDUwODMwMDNaMBQxEjAQBgNVBAMMCVRlc3Qg
                    Q2VydDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMa8bs20RNMPvd2H
                    ToJFaDJ0ZhEE0116/aPzkTE6hyn4guIoaiEcbZOOQeGgdAMluIT1uE92huWI0E33
                    XmBhegr/8AqJ+gUQIOARzlLVJLCiG6SOefBP+OYvbJmPYpUNujU9Ul2soqNBpwe1
                    9zsMV2DHMDZggPeQzfwNuzmsC09o3Rn66Hq/KbAmmdjpqH0e7s+uVYa0sc1fWBp2
                    1/TR/quMtkxG6c5aEgTAI1B4PvMace7uvr5tljyCVIHCViGpMdPjCHxQQlAScivk
                    rwHujUfjZ5PyqfA4uVxYp/2gb4rCTQ8y84g8BEOgdWD96iVM6MU1OC+GxvQqRmkr
                    qBJWSTECAwEAAaNTMFEwHQYDVR0OBBYEFL5aET7chEXiKAvIrwEMYMWFW9ZMMB8G
                    A1UdIwQYMBaAFL5aET7chEXiKAvIrwEMYMWFW9ZMMA8GA1UdEwEB/wQFMAMBAf8w
                    DQYJKoZIhvcNAQELBQADggEBAFiajX+vO/4q1ADK9xkw3WEYDxX92SxB69d3I1Ol
                    E7U8+Rd6NdCoMyUF7ZRZlnvpNJfBzpR+svWOGrmxNPn8jAkXGXdmYx35PGWRsmb4
                    qMntTbUv0FTgEu1NUY3tqMoTNltW+LYUw1Zf7jP98kFGbtztc10zyZhcUNOGJVS7
                    7W6vwVzMF6H9OaxM5W4Uyk+A4qV7rh/AKLdxV5dkzoaEsijCIKyGwDw8XMbtuIrM
                    rcOGJvvtddDpG5aBqaRCU74oHth9rdhMb+dh9eJN2OJxAsVnUNqwLVq2ZloU6mGy
                    AjT35C+tZZBUAVTfTrazGr0DB3aXA67BRr0XBKqP0VYxDgkxggI+MIICOgIBATAs
                    MBQxEjAQBgNVBAMMCVRlc3QgQ2VydAIUP21U+8s8hQ4NbTAJRYNHJC/oabkwDQYJ
                    YIZIAWUDBAIBBQCggeQwGAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAcBgkqhkiG
                    9w0BCQUxDxcNMjUwOTA0MTA0NDA4WjAvBgkqhkiG9w0BCQQxIgQgHjF7Iwi+otOZ
                    lONiQ0h2Sa6R31ReVJRENbFxnCKcF00weQYJKoZIhvcNAQkPMWwwajALBglghkgB
                    ZQMEASowCwYJYIZIAWUDBAEWMAsGCWCGSAFlAwQBAjAKBggqhkiG9w0DBzAOBggq
                    hkiG9w0DAgICAIAwDQYIKoZIhvcNAwICAUAwBwYFKw4DAgcwDQYIKoZIhvcNAwIC
                    ASgwDQYJKoZIhvcNAQEBBQAEggEAni7xKsuLKHkGty2//ZU4xtacW0QJuCs3NZGw
                    KhG8SmsDPnCQIk46uO8hv8Hlw3+bnh2DcdIRRXVsnTz0evw8zDFdOvFNldNK6z5F
                    zdyeroZgLIOEM1I0MgYOWYtmGT+t2rwzPcggrCdA0TXSNvittv8CmneUrPlpmW/+
                    z7HKG7aZzuGdaMjtBJ9SxwADfei/2DkfW70nL6+ZHDiwf944wxSzCilZGNaPQn78
                    mox//ns1p6xs1SNcjqy/yj6avdFRwytuo0CCdmatdpqjH5TW7KN2BDz2x+pLi3XW
                    az4WPLcq14L5YKQ2XREwR6Z1WrXoLliRHAvFzvqTO70eDf7ppjEA
                    -----END PKCS7-----
                    """.trimIndent(),
                )
            }
        exception.message shouldBe "Signature missing."
    }

    @Test
    fun `file with other signature is rejected`() {
        val exception =
            shouldThrow<InvalidLicenseException> {
                sut.saveLicense(
                    """
                    -----BEGIN PKCS7-----
                    MIIGHgYJKoZIhvcNAQcCoIIGDzCCBgsCAQExDzANBglghkgBZQMEAgEFADCBoQYJ
                    KoZIhvcNAQcBoIGTBIGQQ29udGVudC1UeXBlOiB0ZXh0L3BsYWluDQoNCnsiY3Vz
                    dG9tZXJObyI6IjEyMzQiLCJ0b3RhbENsaWVudHMiOjAsInRvdGFsVW5pdHMiOjAs
                    InRvdGFsVXNlcnMiOjAsInZhbGlkVW50aWwiOiIyMDAwLTAxLTAxVDAwOjAwOjAw
                    LjAwMDAwMDAwMFoifQ0KoIIDDTCCAwkwggHxoAMCAQICFHVg7e/CYS3qTBCyD9OC
                    3ieRwzSRMA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNVBAMMCVRlc3QgQ2VydDAeFw0y
                    NTA5MDUwNzQxNTdaFw0yNTA5MDYwNzQxNTdaMBQxEjAQBgNVBAMMCVRlc3QgQ2Vy
                    dDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKmlEA9FGGMuTyczX2ex
                    O+b7k4kWEqP9hd6l6CP869Y29d3AqchGaF0dNY8S8V8wXtJUOV5hjrMzTJ/390y6
                    PcdRz3Uk6N1Zu4kRbecRUVhF3IOxEeOuh4AkvI/qPqMzeUGW64lF7TMYt5KkGiZb
                    3H/Sr+CAmbntdyVQvWf0DD9DDZ9rh6sK3ktDuVFzbvq2h0p3U3gehA4ycEhqOFxu
                    BtsEm23SoIa7uLzMzIStm9vN/ru3Lprs5Q7Flc1Dbu7eNLDAmdAKpRFc/dTJIb2B
                    Pm+1CWnH0h63f2glfinXt2fdmsi6Tm8GTnJ9yd/L28UR6GSISVQnbRb5A/N5fRZW
                    zokCAwEAAaNTMFEwHQYDVR0OBBYEFIsJ/T9V0llPdFBIAd26pB5D0uQBMB8GA1Ud
                    IwQYMBaAFIsJ/T9V0llPdFBIAd26pB5D0uQBMA8GA1UdEwEB/wQFMAMBAf8wDQYJ
                    KoZIhvcNAQELBQADggEBAJQFMNJchOvppoAznfa3MOxROIaZzIBTUcUrBvPBMCaV
                    jg4V4UHAEOQ9gz8xaXVnlBnZD3o6SD39csD+7VeVIASqx+LK/T90EYsnQPEm/M6f
                    pC+HxWThaturCIjGijsvgUCribPwP3bBoXI+wIiU8Zj98z0uOwjLLFrLijiQ+iLe
                    iBUjdRHLC/U53Ot81aMmuxF1izOcXRZCvquFSZPoUMrRLheIXDkeyUT+uNGTRN5p
                    w1BDlk+Skdz9n43z9K1lxnuZ/nHI+MaBtQzTHVD22taLXMUK4gHSeQ20b07S/cb1
                    y37gUQBQBwS5lQXanmpwq0QQ4SQZB//y35zZ9CifrasxggI+MIICOgIBATAsMBQx
                    EjAQBgNVBAMMCVRlc3QgQ2VydAIUdWDt78JhLepMELIP04LeJ5HDNJEwDQYJYIZI
                    AWUDBAIBBQCggeQwGAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAcBgkqhkiG9w0B
                    CQUxDxcNMjUwOTA1MDc0MjAzWjAvBgkqhkiG9w0BCQQxIgQgPCg9FRLmvxPvlKfa
                    tMw7FogwXiWFocpjJLgmhhz1lMIweQYJKoZIhvcNAQkPMWwwajALBglghkgBZQME
                    ASowCwYJYIZIAWUDBAEWMAsGCWCGSAFlAwQBAjAKBggqhkiG9w0DBzAOBggqhkiG
                    9w0DAgICAIAwDQYIKoZIhvcNAwICAUAwBwYFKw4DAgcwDQYIKoZIhvcNAwICASgw
                    DQYJKoZIhvcNAQEBBQAEggEAYqKG/RxXIYqWdDoSEpEK0T5HYxYy4Kk59nGDzsFH
                    cUM3zUrTbSoC6qgS3iggbACG0v0PiJQrR9YpNoPJZdmDJnKxoFwZesIOOMOVYRkA
                    mQshaY+mHQXVB1huBoZQUsb70HvxY+CEczLfYJLRwzolEBJlEXJU73xuL2l+FcIw
                    eduaE6HzgvSeko2dOMUCS8EJELV23EkWvqXXfFZxQA7bkmr0vvPabpzhEAdMZNqL
                    uYWpBn8sIofAGhBwyebJIvdOhMYfpuj0Tas+i4uFCSBN4gk1nN+y0i0xxLAhFfW/
                    b3KbWdLXwmjeXZdJBkLLisc5Q912phGzC7abfwcLMLWWlw==
                    -----END PKCS7-----
                    """.trimIndent(),
                )
            }
        exception.message shouldBe "Invalid signature."
    }

    @Test
    fun `invalid license is rejected`() {
        val exception =
            shouldThrow<InvalidLicenseException> {
                sut.saveLicense(
                    "foo",
                )
            }
        exception.message shouldBe "Request body does not represent a valid license."
    }

    @Test
    fun `expired license is rejected`() {
        val exception =
            shouldThrow<InvalidLicenseException> {
                sut.saveLicense(
                    """
                    -----BEGIN PKCS7-----
                    MIIGHgYJKoZIhvcNAQcCoIIGDzCCBgsCAQExDzANBglghkgBZQMEAgEFADCBoQYJ
                    KoZIhvcNAQcBoIGTBIGQQ29udGVudC1UeXBlOiB0ZXh0L3BsYWluDQoNCnsiY3Vz
                    dG9tZXJObyI6IjEyMzQiLCJ0b3RhbENsaWVudHMiOjAsInRvdGFsVW5pdHMiOjAs
                    InRvdGFsVXNlcnMiOjAsInZhbGlkVW50aWwiOiIyMDAwLTAxLTAxVDAwOjAwOjAw
                    LjAwMDAwMDAwMFoifQ0KoIIDDTCCAwkwggHxoAMCAQICFD9tVPvLPIUODW0wCUWD
                    RyQv6Gm5MA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNVBAMMCVRlc3QgQ2VydDAeFw0y
                    NTA5MDQwODMwMDNaFw0yNTA5MDUwODMwMDNaMBQxEjAQBgNVBAMMCVRlc3QgQ2Vy
                    dDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMa8bs20RNMPvd2HToJF
                    aDJ0ZhEE0116/aPzkTE6hyn4guIoaiEcbZOOQeGgdAMluIT1uE92huWI0E33XmBh
                    egr/8AqJ+gUQIOARzlLVJLCiG6SOefBP+OYvbJmPYpUNujU9Ul2soqNBpwe19zsM
                    V2DHMDZggPeQzfwNuzmsC09o3Rn66Hq/KbAmmdjpqH0e7s+uVYa0sc1fWBp21/TR
                    /quMtkxG6c5aEgTAI1B4PvMace7uvr5tljyCVIHCViGpMdPjCHxQQlAScivkrwHu
                    jUfjZ5PyqfA4uVxYp/2gb4rCTQ8y84g8BEOgdWD96iVM6MU1OC+GxvQqRmkrqBJW
                    STECAwEAAaNTMFEwHQYDVR0OBBYEFL5aET7chEXiKAvIrwEMYMWFW9ZMMB8GA1Ud
                    IwQYMBaAFL5aET7chEXiKAvIrwEMYMWFW9ZMMA8GA1UdEwEB/wQFMAMBAf8wDQYJ
                    KoZIhvcNAQELBQADggEBAFiajX+vO/4q1ADK9xkw3WEYDxX92SxB69d3I1OlE7U8
                    +Rd6NdCoMyUF7ZRZlnvpNJfBzpR+svWOGrmxNPn8jAkXGXdmYx35PGWRsmb4qMnt
                    TbUv0FTgEu1NUY3tqMoTNltW+LYUw1Zf7jP98kFGbtztc10zyZhcUNOGJVS77W6v
                    wVzMF6H9OaxM5W4Uyk+A4qV7rh/AKLdxV5dkzoaEsijCIKyGwDw8XMbtuIrMrcOG
                    JvvtddDpG5aBqaRCU74oHth9rdhMb+dh9eJN2OJxAsVnUNqwLVq2ZloU6mGyAjT3
                    5C+tZZBUAVTfTrazGr0DB3aXA67BRr0XBKqP0VYxDgkxggI+MIICOgIBATAsMBQx
                    EjAQBgNVBAMMCVRlc3QgQ2VydAIUP21U+8s8hQ4NbTAJRYNHJC/oabkwDQYJYIZI
                    AWUDBAIBBQCggeQwGAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAcBgkqhkiG9w0B
                    CQUxDxcNMjUwOTA0MTA1MjE0WjAvBgkqhkiG9w0BCQQxIgQgPCg9FRLmvxPvlKfa
                    tMw7FogwXiWFocpjJLgmhhz1lMIweQYJKoZIhvcNAQkPMWwwajALBglghkgBZQME
                    ASowCwYJYIZIAWUDBAEWMAsGCWCGSAFlAwQBAjAKBggqhkiG9w0DBzAOBggqhkiG
                    9w0DAgICAIAwDQYIKoZIhvcNAwICAUAwBwYFKw4DAgcwDQYIKoZIhvcNAwICASgw
                    DQYJKoZIhvcNAQEBBQAEggEANxJyaPFUw8N4R7pqciKPSYHxpyi6ydh/ulKXMeIp
                    Nmagr6rzJCRViAFcizaQiiXWdM5SEy8OX5Y5OXqqgyQ8maehtR07gSk3NzbTFQuB
                    TlUELzdMgtnyDZYi3X3EKZhQLpGekAMyGxTnHzCiQvjeF6Z3Shk4ttuVEbzFCjWg
                    1dwrObh0tjrRv1aAxWi0B6gOo08QliikZy5UQQRwxBqUFsr25cfcJV2sfHq4zaae
                    OEQ/mzWOnDoDw5Epdi20e5SXvugCur5bVZnwj0giugPfhlfHhPZqVtdYw++h8lsn
                    PvLPGB+nbNUqkEXgf/bGZfDSbaE53c1bS7BFEFltqNf0Zw==
                    -----END PKCS7-----
                    """.trimIndent(),
                )
            }
        exception.message shouldBe "License expired."
    }
}
