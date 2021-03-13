package com.makentoshe.schatbot

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.types.*
import space.jetbrains.yana.command
import space.jetbrains.yana.readPayload

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Routing) {
        chatbot()
    }
}

/**
 * This method handles POST requests on the /api/chatbot endpoint.
 *
 * readPayload(body: String) is the SDK helper function that receives JSON data from Space
 * and deserializes it into ApplicationPayload.
 * For example, in our case, the raw data from Space could look like follows:
 */
fun Routing.chatbot() {
    post("api/chatbot") {
        // read payload and verify Space instance
        val payload = readPayload(call.receiveText())
        if (!Endpoint.verify(payload)) {
            return@post call.respond(HttpStatusCode.Unauthorized)
        }

        try {
            processChatbotPayload(payload)
        } catch (unknownCommand: IllegalStateException) {
            LoggerFactory.getLogger("Chatbot").error(unknownCommand.message)
        }
    }
}

/** Performs a simple payload analysis for future processing */
private suspend fun PipelineContext<*, ApplicationCall>.processChatbotPayload(payload: ApplicationPayload) {
    when (payload) {
        // MessagePayload = user sends a message
        is MessagePayload -> {
            processChatbotMessagePayload(payload)
        }
        // MessageActionPayload = user interacts with interactive element in chat
        is MessageActionPayload -> {
            processChatbotMessageActionPayload(payload)
        }
        // ListCommandsPayload = user types a slash or a char
        is ListCommandsPayload -> {
            processChatbotListCommandsPayload(payload)
        }
    }
}

/**
 * The standard payload that contains the command and command arguments.
 * Actually, the bot receives MessagePayload when a user types something in the chat and presses Enter.
 */
private suspend fun PipelineContext<*, ApplicationCall>.processChatbotMessagePayload(payload: MessagePayload) {
    Commands.list.find { it.name == payload.command() }?.action?.invoke(payload)
        ?: return call.respond(HttpStatusCode.NotFound)
    call.respond(HttpStatusCode.OK)
}

/**
 * The bot receives this payload when a user hits the / slash button and starts to input command char by char.
 * The bot must respond with a list of available commands.
 */
private suspend fun PipelineContext<*, ApplicationCall>.processChatbotListCommandsPayload(payload: ListCommandsPayload) {
    call.respondText(ObjectMapper().writeValueAsString(Commands.commands), ContentType.Application.Json)
}

/**
 * This payload receives after the user performs an interaction with the last interactive element in the chat.
 * For the previous elements their data will be erased (for example, their actionId will be always an empty string)
 */
private suspend fun PipelineContext<*, ApplicationCall>.processChatbotMessageActionPayload(payload: MessageActionPayload) {
    printToChat(userContext(payload), message { section { text(payload.actionId) } })
    call.respond(HttpStatusCode.OK)
}
