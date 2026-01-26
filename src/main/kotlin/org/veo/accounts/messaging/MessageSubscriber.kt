/*
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

import mu.KotlinLogging
import org.springframework.amqp.rabbit.annotation.Argument
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.veo.accounts.dtos.UnitId
import org.veo.accounts.dtos.VeoClientId
import org.veo.accounts.keycloak.GroupService
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID

private val log = KotlinLogging.logger {}
private val om = jacksonObjectMapper()

@Component
@ConditionalOnProperty(value = ["veo.accounts.rabbitmq.subscribe"], havingValue = "true")
class MessageSubscriber(
    private val groupService: GroupService,
) {
    @RabbitListener(
        bindings = [
            QueueBinding(
                value =
                    Queue(
                        value = "\${veo.accounts.rabbitmq.queues.veo-subscriptions}",
                        exclusive = "false",
                        durable = "true",
                        autoDelete = "\${veo.accounts.rabbitmq.queue.autoDelete}",
                        arguments = [Argument(name = "x-dead-letter-exchange", value = "\${veo.accounts.rabbitmq.dlx}")],
                    ),
                exchange = Exchange(value = "\${veo.accounts.rabbitmq.exchanges.veo-subscriptions}", type = "topic"),
                key = [
                    "\${veo.accounts.rabbitmq.routing_key_prefix}client_change",
                ],
            ), QueueBinding(
                value =
                    Queue(
                        value = "\${veo.accounts.rabbitmq.queues.veo}",
                        exclusive = "false",
                        durable = "true",
                        autoDelete = "\${veo.accounts.rabbitmq.queue.autoDelete}",
                        arguments = [Argument(name = "x-dead-letter-exchange", value = "\${veo.accounts.rabbitmq.dlx}")],
                    ),
                exchange = Exchange(value = "\${veo.accounts.rabbitmq.exchanges.veo}", type = "topic"),
                key = [
                    "\${veo.accounts.rabbitmq.routing_key_prefix}unit_deletion",
                ],
            ),
        ],
    )
    fun handleMessage(message: String) =
        try {
            om
                .readTree(message)
                .get("content")
                .asString()
                .let(om::readTree)
                .let { handleMessage(it) }
        } catch (ex: Throwable) {
            log.error(ex) { "Handling failed for message: '$message'" }
            throw RuntimeException(ex)
        }

    private fun handleMessage(content: JsonNode) {
        content
            .get("eventType")
            .asString()
            .let {
                log.debug { "Received message with '$it' event" }
                when (it) {
                    "client_change" -> handleClientChange(content)
                    "unit_deletion" -> handleUnitDeletion(content)
                    else -> throw NotImplementedError("Unsupported event type $it")
                }
            }
    }

    private fun handleClientChange(content: JsonNode) {
        val client =
            content
                .get("clientId")
                .asString()
                .let { UUID.fromString(it) }
                .let { VeoClientId(it) }
        when (content.get("type").asString()) {
            "ACTIVATION" -> groupService.activateClient(client)
            "CREATION" ->
                groupService.createClient(
                    client,
                    content.get("maxUnits").asInt(),
                    content.get("maxUsers").asInt(),
                )
            "MODIFICATION" ->
                groupService.updateClient(
                    client,
                    content.get("maxUnits")?.asInt(),
                    content.get("maxUsers")?.asInt(),
                )
            "DEACTIVATION" -> groupService.deactivateClient(client)
            "DELETION" -> groupService.deleteClient(client)
        }
    }

    private fun handleUnitDeletion(content: JsonNode) {
        val client =
            content
                .get("clientId")
                .asString()
                .let { UUID.fromString(it) }
                .let { VeoClientId(it) }
        val unit =
            content
                .get("unitId")
                .asString()
                .let { UUID.fromString(it) }
                .let { UnitId(it) }
        groupService.removeUnitRights(unit, client)
    }
}
