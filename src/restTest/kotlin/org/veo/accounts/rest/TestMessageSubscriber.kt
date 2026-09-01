/*
 * verinice.veo accounts
 * Copyright (C) 2026  Jonas Jordan
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

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.veo.accounts.asMap

private val log = logger {}

@Component
class TestMessageSubscriber {
    val receivedMessages = mutableListOf<Map<String, *>>()

    @RabbitListener(
        bindings = [
            QueueBinding(
                value =
                    Queue(
                        value = "\${veo.accounts.rabbitmq.queues.veo-subscriptions}-test",
                        exclusive = "false",
                        durable = "true",
                        autoDelete = "\${veo.accounts.rabbitmq.queue.autoDelete}",
                    ),
                exchange = Exchange(value = "\${veo.accounts.rabbitmq.exchanges.veo-subscriptions}", type = "topic"),
                key = [
                    "\${veo.accounts.rabbitmq.routing_key_prefix}#",
                ],
            ),
        ],
    )
    fun handleMessage(message: String) {
        log.info { "Received message: $message" }
        receivedMessages.add(
            om
                .readTree(message)
                .get("content")
                .asString()
                .let { om.readValue(it, Any::class.java).asMap() },
        )
    }
}
