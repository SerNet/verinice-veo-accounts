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
package org.veo.accounts.rest

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.veo.accounts.Role

class LicenseRestTest : AbstractRestTest() {
    lateinit var managerId: String

    @BeforeEach
    fun setup() {
        managerId = createManager(createVeoClientGroup(), listOf(Role.SET_LICENSE_KEY))
    }

    @Test
    fun `file without signature is rejected`() {
        putRaw(
            "/admin/license",
            managerId,
            """
            -----BEGIN PKCS7-----
            MIAGCSqGSIb3DQEHAqCAMIACAQExADCABgkqhkiG9w0BBwGggCSABHJ7ImN1c3Rv
            bWVyTm8iOiIxMjM0IiwidG90YWxDbGllbnRzIjowLCJ0b3RhbFVuaXRzIjowLCJ0
            b3RhbFVzZXJzIjowLCJ2YWxpZFVudGlsIjoiMzAwMC0wMS0wMVQwMDowMDowMC4w
            MDAwMDAwMDBaIn0AAAAAAAAxAAAAAAAAAA==
            -----END PKCS7-----
            """.trimIndent(),
            422,
            mapOf("Content-Type" to listOf("application/pkcs7-mime")),
        ).rawBody shouldBe "Signature missing."
    }

    @Test
    fun `file with other signature is rejected`() {
        putRaw(
            "/admin/license",
            managerId,
            """
            -----BEGIN PKCS7-----
            MIIF/gYJKoZIhvcNAQcCoIIF7zCCBesCAQExDzANBglghkgBZQMEAgEFADCBgQYJ
            KoZIhvcNAQcBoHQEcnsiY3VzdG9tZXJObyI6IjEyMzQiLCJ0b3RhbENsaWVudHMi
            OjAsInRvdGFsVW5pdHMiOjAsInRvdGFsVXNlcnMiOjAsInZhbGlkVW50aWwiOiIz
            MDAwLTAxLTAxVDAwOjAwOjAwLjAwMDAwMDAwMFoifaCCAw0wggMJMIIB8aADAgEC
            AhR1YO3vwmEt6kwQsg/Tgt4nkcM0kTANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQD
            DAlUZXN0IENlcnQwHhcNMjUwOTA1MDc0MTU3WhcNMjUwOTA2MDc0MTU3WjAUMRIw
            EAYDVQQDDAlUZXN0IENlcnQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB
            AQCppRAPRRhjLk8nM19nsTvm+5OJFhKj/YXepegj/OvWNvXdwKnIRmhdHTWPEvFf
            MF7SVDleYY6zM0yf9/dMuj3HUc91JOjdWbuJEW3nEVFYRdyDsRHjroeAJLyP6j6j
            M3lBluuJRe0zGLeSpBomW9x/0q/ggJm57XclUL1n9Aw/Qw2fa4erCt5LQ7lRc276
            todKd1N4HoQOMnBIajhcbgbbBJtt0qCGu7i8zMyErZvbzf67ty6a7OUOxZXNQ27u
            3jSwwJnQCqURXP3UySG9gT5vtQlpx9Iet39oJX4p17dn3ZrIuk5vBk5yfcnfy9vF
            EehkiElUJ20W+QPzeX0WVs6JAgMBAAGjUzBRMB0GA1UdDgQWBBSLCf0/VdJZT3RQ
            SAHduqQeQ9LkATAfBgNVHSMEGDAWgBSLCf0/VdJZT3RQSAHduqQeQ9LkATAPBgNV
            HRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQCUBTDSXITr6aaAM532tzDs
            UTiGmcyAU1HFKwbzwTAmlY4OFeFBwBDkPYM/MWl1Z5QZ2Q96Okg9/XLA/u1XlSAE
            qsfiyv0/dBGLJ0DxJvzOn6Qvh8Vk4WrbqwiIxoo7L4FAq4mz8D92waFyPsCIlPGY
            /fM9LjsIyyxay4o4kPoi3ogVI3URywv1OdzrfNWjJrsRdYsznF0WQr6rhUmT6FDK
            0S4XiFw5HslE/rjRk0TeacNQQ5ZPkpHc/Z+N8/StZcZ7mf5xyPjGgbUM0x1Q9trW
            i1zFCuIB0nkNtG9O0v3G9ct+4FEAUAcEuZUF2p5qcKtEEOEkGQf/8t+c2fQon62r
            MYICPjCCAjoCAQEwLDAUMRIwEAYDVQQDDAlUZXN0IENlcnQCFHVg7e/CYS3qTBCy
            D9OC3ieRwzSRMA0GCWCGSAFlAwQCAQUAoIHkMBgGCSqGSIb3DQEJAzELBgkqhkiG
            9w0BBwEwHAYJKoZIhvcNAQkFMQ8XDTI1MDkyMjEyNTkwM1owLwYJKoZIhvcNAQkE
            MSIEIGaZ1yK+awF+HfTfJij96Z2TTnouqOSgVy49dH8jO3ueMHkGCSqGSIb3DQEJ
            DzFsMGowCwYJYIZIAWUDBAEqMAsGCWCGSAFlAwQBFjALBglghkgBZQMEAQIwCgYI
            KoZIhvcNAwcwDgYIKoZIhvcNAwICAgCAMA0GCCqGSIb3DQMCAgFAMAcGBSsOAwIH
            MA0GCCqGSIb3DQMCAgEoMA0GCSqGSIb3DQEBAQUABIIBAH7bY7hQ57W/ALGZ2t26
            jGNAb3Ia8y9c4q4V5dMEHmqZ4lIUStdRxGWTo8TFm98CD+SnPmpMm4vEpaxC80xv
            VEeE7HvyhXtwWpTZGbqEYrpQB1QJotYh1K+BBLJwHk3l8SO7bgwDxBG6uZ/J3Dad
            Cl+Wzh6fUq17UGsAegdL8OKzrZJSaORoM7AgspMbr5vKOUcl1x3qSAYLE9QXa2ys
            IOTton1No1mIGKGdgWDbXUDdEZMi0c4pBoSfbfMOxObg2ZWzDWj44rbOL7bN6zkx
            rckvWWs14Tso/oSPMgpLOe8DY5OBnOSD6nCLQ5E5vKfisNIKhDJhLyk1Dka9jqJf
            CL4=
            -----END PKCS7-----
            """.trimIndent(),
            422,
            mapOf("Content-Type" to listOf("application/pkcs7-mime")),
        ).rawBody shouldBe "Invalid signature."
    }

    @Test
    fun `invalid license is rejected`() {
        putRaw(
            "/admin/license",
            managerId,
            "foo",
            422,
            mapOf("Content-Type" to listOf("application/pkcs7-mime")),
        ).rawBody shouldBe "Request body does not represent a valid license."
    }

    @Test
    fun `expired license is rejected`() {
        putRaw(
            "/admin/license",
            managerId,
            """
            -----BEGIN PKCS7-----
            MIIF/gYJKoZIhvcNAQcCoIIF7zCCBesCAQExDzANBglghkgBZQMEAgEFADCBgQYJ
            KoZIhvcNAQcBoHQEcnsiY3VzdG9tZXJObyI6IjEyMzQiLCJ0b3RhbENsaWVudHMi
            OjAsInRvdGFsVW5pdHMiOjAsInRvdGFsVXNlcnMiOjAsInZhbGlkVW50aWwiOiIy
            MDAwLTAxLTAxVDAwOjAwOjAwLjAwMDAwMDAwMFoifaCCAw0wggMJMIIB8aADAgEC
            AhQ/bVT7yzyFDg1tMAlFg0ckL+hpuTANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQD
            DAlUZXN0IENlcnQwHhcNMjUwOTA0MDgzMDAzWhcNMjUwOTA1MDgzMDAzWjAUMRIw
            EAYDVQQDDAlUZXN0IENlcnQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB
            AQDGvG7NtETTD73dh06CRWgydGYRBNNdev2j85ExOocp+ILiKGohHG2TjkHhoHQD
            JbiE9bhPdobliNBN915gYXoK//AKifoFECDgEc5S1SSwohukjnnwT/jmL2yZj2KV
            Dbo1PVJdrKKjQacHtfc7DFdgxzA2YID3kM38Dbs5rAtPaN0Z+uh6vymwJpnY6ah9
            Hu7PrlWGtLHNX1gadtf00f6rjLZMRunOWhIEwCNQeD7zGnHu7r6+bZY8glSBwlYh
            qTHT4wh8UEJQEnIr5K8B7o1H42eT8qnwOLlcWKf9oG+Kwk0PMvOIPARDoHVg/eol
            TOjFNTgvhsb0KkZpK6gSVkkxAgMBAAGjUzBRMB0GA1UdDgQWBBS+WhE+3IRF4igL
            yK8BDGDFhVvWTDAfBgNVHSMEGDAWgBS+WhE+3IRF4igLyK8BDGDFhVvWTDAPBgNV
            HRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBYmo1/rzv+KtQAyvcZMN1h
            GA8V/dksQevXdyNTpRO1PPkXejXQqDMlBe2UWZZ76TSXwc6UfrL1jhq5sTT5/IwJ
            Fxl3ZmMd+TxlkbJm+KjJ7U21L9BU4BLtTVGN7ajKEzZbVvi2FMNWX+4z/fJBRm7c
            7XNdM8mYXFDThiVUu+1ur8FczBeh/TmsTOVuFMpPgOKle64fwCi3cVeXZM6GhLIo
            wiCshsA8PFzG7biKzK3Dhib77XXQ6RuWgamkQlO+KB7Yfa3YTG/nYfXiTdjicQLF
            Z1DasC1atmZaFOphsgI09+QvrWWQVAFU3062sxq9Awd2lwOuwUa9FwSqj9FWMQ4J
            MYICPjCCAjoCAQEwLDAUMRIwEAYDVQQDDAlUZXN0IENlcnQCFD9tVPvLPIUODW0w
            CUWDRyQv6Gm5MA0GCWCGSAFlAwQCAQUAoIHkMBgGCSqGSIb3DQEJAzELBgkqhkiG
            9w0BBwEwHAYJKoZIhvcNAQkFMQ8XDTI1MDkyMjEyMzUzMlowLwYJKoZIhvcNAQkE
            MSIEIHwcMvVvu1+cQPccDBOtShwFdvlpQKxwfIu0RUgdD+ZSMHkGCSqGSIb3DQEJ
            DzFsMGowCwYJYIZIAWUDBAEqMAsGCWCGSAFlAwQBFjALBglghkgBZQMEAQIwCgYI
            KoZIhvcNAwcwDgYIKoZIhvcNAwICAgCAMA0GCCqGSIb3DQMCAgFAMAcGBSsOAwIH
            MA0GCCqGSIb3DQMCAgEoMA0GCSqGSIb3DQEBAQUABIIBADdYdaHt2WNvKI9hDDie
            WsxtQGaqzaaQuzXlPDmMAAnpb1lqYNKwIOOyIavc5s5Hj1OnZUGoxuil1ixr5WzE
            kVxnyzxlvDoH2GKL1blyUH9TnNxQtn6QpbJQriPr+ifWeXB1CT00OezWcWJkV3Ga
            yWtddmxa4FLAme0jpCP2fUiucdb59tRXDUKNnxg2rc7xvqAQCHLyKUL+zvIdSmcO
            oMZw8Ays57dmbRzm7YTysemazkZrex5gcNg54XANHhRQPp5ka6SqGwlrpiVS+U7r
            rTsDN4lCcGNyiDMPNpuOsTj7nhjjOXYnOKF+yRTOBvslbuIuDQvY5sehT7yEaSzU
            itA=
            -----END PKCS7-----
            """.trimIndent(),
            422,
            mapOf("Content-Type" to listOf("application/pkcs7-mime")),
        ).rawBody shouldBe "License expired."
    }
}
