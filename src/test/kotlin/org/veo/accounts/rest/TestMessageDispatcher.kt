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
import io.mockk.InternalPlatformDsl.toStr
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.lang.Math.random

val om = jacksonObjectMapper()

@Component
class TestMessageDispatcher(
    @Value("\${veo.accounts.rabbitmq.exchange}")
    private val exchange: String,
    @Value("\${veo.accounts.rabbitmq.subscription_routing_key_prefix}")
    private val subscriptionRoutingKeyPrefix: String,
    private val rabbitTemplate: RabbitTemplate
) {
    fun sendMessage(routingKey: String, content: Map<String, *>) {
        send(
            "$subscriptionRoutingKeyPrefix$routingKey",
            (random() * Long.MAX_VALUE).toLong(),
            om.writeValueAsString(content)
        )
    }

    private fun send(routingKey: String, id: Long, content: String) {
        rabbitTemplate.convertSendAndReceive(
            exchange,
            routingKey,
            om.writeValueAsString(mapOf("content" to content)),
            CorrelationData(id.toStr())
        )
    }
}
