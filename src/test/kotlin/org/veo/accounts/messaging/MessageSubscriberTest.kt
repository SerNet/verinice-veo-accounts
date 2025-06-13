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
package org.veo.accounts.messaging

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.keycloak.GroupService
import java.util.UUID
import kotlin.reflect.full.functions

private val om = jacksonObjectMapper()

class MessageSubscriberTest {
    private val groupService = mockk<GroupService>()
    private val sut = MessageSubscriber(groupService)

    @Test
    fun `listeners don't return anything`() {
        MessageSubscriber::class
            .functions
            .filter { it.annotations.filterIsInstance<RabbitListener>().isNotEmpty() }
            .forEach { it.returnType.toStr() shouldBe "kotlin.Unit" }
    }

    @Test
    fun `handles client creation`() {
        every { groupService.createClient(any(), any(), any()) } just Runs

        // when a client deletion message is received
        sut.handleMessage(
            message(
                "eventType" to "client_change",
                "clientId" to "cc12aad0-b9fb-46a0-9beb-489ed40ebb24",
                "maxUnits" to 6,
                "maxUsers" to 7,
                "type" to "CREATION",
            ),
        )

        // then the client is created
        verify { groupService.createClient(VeoClientId(UUID.fromString("cc12aad0-b9fb-46a0-9beb-489ed40ebb24")), 6, 7) }
    }

    @Test
    fun `handles client activation`() {
        every { groupService.activateClient(any()) } just Runs

        // when a client deletion message is received
        sut.handleMessage(
            message(
                "eventType" to "client_change",
                "clientId" to "cc12aad0-b9fb-46a0-9beb-489ed40ebb24",
                "type" to "ACTIVATION",
            ),
        )

        // then the client is activated
        verify { groupService.activateClient(VeoClientId(UUID.fromString("cc12aad0-b9fb-46a0-9beb-489ed40ebb24"))) }
    }

    @Test
    fun `handles client deactivation`() {
        every { groupService.deactivateClient(any()) } just Runs

        // when a client deletion message is received
        sut.handleMessage(
            message(
                "eventType" to "client_change",
                "clientId" to "cc12aad0-b9fb-46a0-9beb-489ed40ebb24",
                "type" to "DEACTIVATION",
            ),
        )

        // then the client is deactivated
        verify { groupService.deactivateClient(VeoClientId(UUID.fromString("cc12aad0-b9fb-46a0-9beb-489ed40ebb24"))) }
    }

    @Test
    fun `handles client deletion`() {
        every { groupService.deleteClient(any()) } just Runs

        // when a client deletion message is received
        sut.handleMessage(
            message(
                "eventType" to "client_change",
                "clientId" to "cc12aad0-b9fb-46a0-9beb-489ed40ebb24",
                "type" to "DELETION",
            ),
        )

        // then the client is deleted
        verify { groupService.deleteClient(VeoClientId(UUID.fromString("cc12aad0-b9fb-46a0-9beb-489ed40ebb24"))) }
    }

    private fun message(vararg properties: Pair<String, Any>): String =
        mutableMapOf<String, Any>()
            .apply { putAll(properties) }
            .let(om::writeValueAsString)
            .let { mapOf("content" to it) }
            .let(om::writeValueAsString)
}
