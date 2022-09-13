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
package org.veo.accounts.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.veo.accounts.Role
import org.veo.accounts.Role.CREATE
import org.veo.accounts.Role.DELETE
import org.veo.accounts.Role.READ
import org.veo.accounts.Role.UPDATE
import org.veo.accounts.VeoAccountsApplication
import org.veo.accounts.WebSecurity
import org.veo.accounts.keycloak.TestAccountService
import java.util.UUID.randomUUID

@ActiveProfiles(value = ["resttest"])
@SpringBootTest(classes = [VeoAccountsApplication::class, WebSecurity::class], webEnvironment = RANDOM_PORT)
abstract class AbstractRestTest {
    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var testAccountService: TestAccountService

    @Autowired
    private lateinit var testAuthenticator: TestAuthenticator

    @Value("\${veo.resttest.baseUrl}")
    private var configuredBaseUrl: String? = null

    /** Prefix usernames & email addresses with this to keep simultaneous rest-test runs on the same keycloak instance isolated. */
    protected val prefix = "veo-accounts-rest-test-run-${randomUUID().toString().substring(0,7)}"

    private val baseUrl: String by lazy { (configuredBaseUrl ?: testRestTemplate.rootUri).trimEnd('/') }

    @AfterEach
    fun teardown() {
        testAccountService.cleanup()
    }

    protected fun createVeoClientGroup(): String = testAccountService.createVeoClientGroup()

    protected fun createAccount(groupId: String, roles: List<Role> = listOf(CREATE, READ, UPDATE, DELETE)): String =
        testAccountService.createAccount(groupId, roles, prefix)

    protected fun options(url: String, authAccountId: String? = null, expectedStatus: Int = 200, headers: Map<String, List<String>> = emptyMap()): Response =
        exchange(HttpMethod.OPTIONS, url, authAccountId, headers = headers, expectedStatus = expectedStatus)

    protected fun get(url: String, authAccountId: String? = null, expectedStatus: Int = 200, headers: Map<String, List<String>> = emptyMap()): Response =
        exchange(HttpMethod.GET, url, authAccountId, headers = headers, expectedStatus = expectedStatus)

    protected fun post(url: String, authAccountId: String? = null, body: Any? = null, expectedStatus: Int = 201): Response =
        exchange(HttpMethod.POST, url, authAccountId, body, expectedStatus = expectedStatus)

    protected fun put(url: String, authAccountId: String? = null, body: Any?, expectedStatus: Int = 204): Response =
        exchange(HttpMethod.PUT, url, authAccountId, body, expectedStatus = expectedStatus)

    protected fun delete(url: String, authAccountId: String? = null, expectedStatus: Int = 204): Response =
        exchange(HttpMethod.DELETE, url, authAccountId, expectedStatus = expectedStatus)

    private fun exchange(
        method: HttpMethod,
        uri: String,
        authAccountId: String?,
        body: Any? = null,
        headers: Map<String, List<String>> = emptyMap(),
        expectedStatus: Int
    ): Response =
        testRestTemplate.exchange(baseUrl + uri, method, buildHttpEntity(body, headers, authAccountId), String::class.java)
            .apply { statusCode.value() shouldBe expectedStatus }
            .let { Response(it) }

    private fun buildHttpEntity(
        body: Any?,
        headerMap: Map<String, List<String>>,
        authAccountId: String?
    ) = HttpEntity(
        body?.let { jacksonObjectMapper().writeValueAsString(it) },
        buildHeaders(headerMap, authAccountId)
    )

    private fun buildHeaders(headerMap: Map<String, List<String>>, authAccountId: String?) = HttpHeaders()
        .apply { set("Content-Type", "application/json") }
        .apply { authAccountId?.let { set("Authorization", "Bearer " + getToken(it)) } }
        .apply { headerMap.forEach { (key, values) -> set(key, values) } }

    private fun getToken(authAccountId: String): String = testAuthenticator.getToken(
        testAccountService.getUsername(authAccountId),
        testAccountService.testPassword
    )
}
