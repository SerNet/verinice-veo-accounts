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
package org.veo.accounts.systemmessages

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.util.Locale

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
class VeoApiServiceTest {
    private val apiUrl = "http://api.veo.example/veo"
    private val apiKey = "secret"

    private val restTemplate =
        mockk<RestTemplate> {
            every {
                exchange("$apiUrl/admin/messages", HttpMethod.POST, any<HttpEntity<SystemMessage>>(), Void::class.java)
            } returns ResponseEntity(HttpStatus.CREATED)

            every {
                exchange(
                    match<String>(Regex("^$apiUrl/admin/messages/\\d+$")::matches),
                    HttpMethod.DELETE,
                    any<HttpEntity<Unit>>(),
                    Void::class.java,
                )
            } returns ResponseEntity(HttpStatus.CREATED)
        }

    private val sut = spyk(VeoApiService(apiUrl, apiKey, restTemplate))

    @Test
    fun `can add license messages`() {
        every {
            restTemplate.exchange("$apiUrl/messages", HttpMethod.GET, any<HttpEntity<Unit>>(), Array<SystemMessage>::class.java)
        } returns ResponseEntity(arrayOf(), HttpStatus.OK)

        sut.setLicenseMessages(setOf(LicenseMessage("Hallo Welt!", "Hello world!", MessageLevel.INFO)))

        verify {
            restTemplate.exchange(
                "$apiUrl/admin/messages",
                HttpMethod.POST,
                HttpEntity(
                    SystemMessage(
                        null,
                        mapOf(Locale.ENGLISH to "Hello world!", Locale.GERMAN to "Hallo Welt!"),
                        setOf("license"),
                        MessageLevel.INFO,
                    ),
                    HttpHeaders().also { it.set("x-api-key", apiKey) } as MultiValueMap<String, String>,
                ),
                Void::class.java,
            )
        }
    }

    @Test
    fun `can replace license messages`() {
        every {
            restTemplate.exchange("$apiUrl/messages", HttpMethod.GET, any<HttpEntity<Unit>>(), Array<SystemMessage>::class.java)
        } returns
            ResponseEntity(
                arrayOf(
                    SystemMessage(
                        "1",
                        mapOf(Locale.ENGLISH to "foo"),
                        setOf("license"),
                        MessageLevel.WARNING,
                    ),
                ),
                HttpStatus.OK,
            )

        sut.setLicenseMessages(setOf(LicenseMessage("Hallo Welt!", "Hello world!", MessageLevel.INFO)))

        verify {
            restTemplate.exchange(
                "$apiUrl/admin/messages/1",
                HttpMethod.DELETE,
                HttpEntity<Unit>(
                    HttpHeaders().also { it.set("x-api-key", apiKey) } as MultiValueMap<String, String>,
                ),
                Void::class.java,
            )

            restTemplate.exchange(
                "$apiUrl/admin/messages",
                HttpMethod.POST,
                HttpEntity(
                    SystemMessage(
                        null,
                        mapOf(Locale.ENGLISH to "Hello world!", Locale.GERMAN to "Hallo Welt!"),
                        setOf("license"),
                        MessageLevel.INFO,
                    ),
                    HttpHeaders().also { it.set("x-api-key", apiKey) } as MultiValueMap<String, String>,
                ),
                Void::class.java,
            )
        }
    }
}
