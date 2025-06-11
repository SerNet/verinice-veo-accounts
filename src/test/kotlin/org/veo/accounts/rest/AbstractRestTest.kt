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
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.keycloak.representations.idm.GroupRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait.forListeningPort
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.veo.accounts.Role
import org.veo.accounts.Role.CREATE
import org.veo.accounts.Role.DELETE
import org.veo.accounts.Role.READ
import org.veo.accounts.Role.UPDATE
import org.veo.accounts.VeoAccountsApplication
import org.veo.accounts.WebSecurity
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.keycloak.TestAccountService
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.Int.Companion.MAX_VALUE

@ActiveProfiles(value = ["resttest"])
@SpringBootTest(classes = [VeoAccountsApplication::class, WebSecurity::class], webEnvironment = RANDOM_PORT)
abstract class AbstractRestTest {
    private val createdVeoClients = mutableListOf<VeoClientId>()

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var testAccountService: TestAccountService

    @Autowired
    private lateinit var testAuthenticator: TestAuthenticator

    @Autowired
    private lateinit var testMessageDispatcher: TestMessageDispatcher

    @Value("\${veo.resttest.baseUrl:#{null}}")
    private var configuredBaseUrl: String? = null

    @Value("\${veo.accounts.auth.apiKeys.clientInit}")
    protected lateinit var clientInitApiKey: String

    companion object {
        private var rabbit: GenericContainer<*>? = null

        @BeforeAll
        @JvmStatic
        fun setupAll() {
            if (rabbit == null && System.getenv("SPRING_RABBITMQ_HOST") == null) {
                rabbit =
                    GenericContainer("rabbitmq:3-management")
                        .withExposedPorts(5672, 15672)
                        .waitingFor(forListeningPort())
                        .apply { start() }
                        .apply {
                            System.getProperties().putAll(
                                mapOf(
                                    "spring.rabbitmq.host" to host,
                                    "spring.rabbitmq.port" to getMappedPort(5672),
                                ),
                            )
                        }
            }
        }
    }

    /** Prefix usernames & email addresses with this to keep simultaneous rest-test runs on the same keycloak instance isolated. */
    protected val prefix = "veo-accounts-rest-test-run-${randomUUID().toString().substring(0,7)}"

    private val baseUrl: String by lazy { (configuredBaseUrl ?: testRestTemplate.rootUri).trimEnd('/') }

    @AfterEach
    fun teardown() {
        createdVeoClients
            .filter { findGroup(it.groupName) != null }
            .forEach {
                sendMessage(
                    "client_change",
                    mapOf(
                        "clientId" to it.clientId,
                        "eventType" to "client_change",
                        "type" to "DELETION",
                    ),
                ) { findGroup(it.groupName) shouldBe null }
            }
    }

    protected fun createVeoClientGroup(
        maxUsers: Int = MAX_VALUE,
        maxUnits: Int = MAX_VALUE,
    ): VeoClientId =
        VeoClientId(randomUUID())
            .apply {
                sendMessage(
                    "client_change",
                    mapOf(
                        "clientId" to clientId,
                        "eventType" to "client_change",
                        "maxUnits" to maxUnits,
                        "maxUsers" to maxUsers,
                        "type" to "CREATION",
                    ),
                ) { findGroup(groupName) shouldNotBe null }
            }.also { createdVeoClients.add(it) }

    protected fun createManager(
        group: VeoClientId,
        roles: List<Role> = listOf(CREATE, READ, UPDATE, DELETE),
    ): String = testAccountService.createManager(group, roles, prefix)

    protected fun updateMaxUsers(
        group: VeoClientId,
        maxUsers: Int,
    ) {
        testAccountService.updateMaxUsers(group, maxUsers)
    }

    protected fun options(
        url: String,
        authAccountId: String? = null,
        expectedStatus: Int? = 200,
        headers: Map<String, List<String>> = emptyMap(),
    ): Response = exchange(HttpMethod.OPTIONS, url, authAccountId, headers = headers, expectedStatus = expectedStatus)

    protected fun get(
        url: String,
        authAccountId: String? = null,
        expectedStatus: Int? = 200,
        headers: Map<String, List<String>> = emptyMap(),
    ): Response = exchange(HttpMethod.GET, url, authAccountId, headers = headers, expectedStatus = expectedStatus)

    protected fun post(
        url: String,
        authAccountId: String? = null,
        body: Any? = null,
        expectedStatus: Int? = 201,
        headers: Map<String, List<String>> = emptyMap(),
    ): Response = postRaw(url, authAccountId, serialize(body), expectedStatus, headers)

    protected fun postRaw(
        url: String,
        authAccountId: String? = null,
        body: String?,
        expectedStatus: Int? = 201,
        headers: Map<String, List<String>> = emptyMap(),
    ): Response = exchange(HttpMethod.POST, url, authAccountId, body, headers, expectedStatus)

    protected fun put(
        url: String,
        authAccountId: String? = null,
        body: Any?,
        expectedStatus: Int? = 204,
        headers: Map<String, List<String>> = emptyMap(),
    ): Response = exchange(HttpMethod.PUT, url, authAccountId, serialize(body), headers, expectedStatus)

    protected fun delete(
        url: String,
        authAccountId: String? = null,
        expectedStatus: Int? = 204,
        headers: Map<String, List<String>> = emptyMap(),
    ): Response = exchange(HttpMethod.DELETE, url, authAccountId, null, headers, expectedStatus)

    protected fun sendMessage(
        routingKey: String,
        content: Map<String, *>,
        completionAssertion: () -> Unit,
    ) {
        testMessageDispatcher.sendMessage(routingKey, content)
        await().atMost(5, SECONDS).until {
            try {
                completionAssertion()
                true
            } catch (ex: Throwable) {
                false
            }
        }
    }

    protected fun accountExists(accountId: String): Boolean = testAccountService.findAccount(accountId) != null

    protected fun findAccount(accountId: String): UserRepresentation? = testAccountService.findAccount(accountId)

    protected fun findAccountGroupNames(accountId: String): List<String> = testAccountService.getAccountGroups(accountId)

    protected fun findGroup(groupName: String): GroupRepresentation? = testAccountService.findGroup(groupName)

    protected fun accountInGroup(
        accountId: String,
        groupName: String,
    ): Boolean = testAccountService.accountInGroup(accountId, groupName)

    private fun exchange(
        method: HttpMethod,
        uri: String,
        authAccountId: String?,
        body: String? = null,
        headers: Map<String, List<String>> = emptyMap(),
        expectedStatus: Int?,
    ): Response =
        testRestTemplate
            .exchange(buildUrl(uri), method, buildHttpEntity(body, headers, authAccountId), String::class.java)
            .let { Response(it) }
            .also {
                if (expectedStatus != null && it.statusCode != expectedStatus) {
                    throw AssertionError(
                        "$method $uri: Expected status code $expectedStatus, but received ${it.statusCode}:\n${it.rawBody}",
                    )
                }
            }

    private fun buildUrl(uri: String): String = if (uri.startsWith("http")) uri else baseUrl + uri

    private fun buildHttpEntity(
        body: String?,
        headerMap: Map<String, List<String>>,
        authAccountId: String?,
    ) = HttpEntity(
        body ?: "",
        buildHeaders(headerMap, authAccountId),
    )

    private fun serialize(body: Any?): String? = body?.let { jacksonObjectMapper().writeValueAsString(it) }

    private fun buildHeaders(
        headerMap: Map<String, List<String>>,
        authAccountId: String?,
    ) = HttpHeaders()
        .apply { set("Content-Type", "application/json") }
        .apply { authAccountId?.let { set("Authorization", "Bearer " + getToken(it)) } }
        .apply { headerMap.forEach { (key, values) -> set(key, values) } }

    private fun getToken(authAccountId: String): String =
        testAuthenticator.getToken(
            testAccountService.getUsername(authAccountId),
            testAccountService.testPassword,
        )
}
