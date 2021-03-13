package com.makentoshe.schatbot

import space.jetbrains.api.runtime.helpers.message
import space.jetbrains.api.runtime.types.*
import space.jetbrains.api.runtime.types.Commands
import space.jetbrains.yana.commandArguments

/**
 * Class for containing command details and action.
 */
data class Command(
    val name: String,
    val info: String,
    val action: suspend (payload: MessagePayload) -> Unit
) {

    /**
     * Part of the protocol - returns info about a command to the chat
     */
    fun toCommandDetail() = CommandDetail(name, info)
}

/**
 * Some kind of utility class for controlling available commands
 */
object Commands {

    val help = Command(
        "help",
        "Show this help",
    ) { payload ->
        val context = HelpContext.from(payload)
        printToChat(context, message {
            section {
                text(
                    """Help message:
                    help - Show this help
                    ${echo.name} - ${echo.info}
                    ${interactive.name} - ${interactive.info}
                """.trimIndent()
                )
            }
        })
    }

    val echo = Command(
        "echo",
        "Echoing the input string",
    ) { payload ->
        val context = EchoContext.from(payload)
        val body = context.message.body
        printToChat(context, message = if (body is ChatMessage.Text) {
            message { section { text(body.text) } }
        } else {
            message { section { text("Skip the Block body") } }
        })
    }

    val interactive = Command(
        "interactive",
        "Displaying available message interactive elements"
    ) { payload ->
        val context = InteractiveContext.from(payload)
        val arguments = payload.commandArguments()
        if (arguments == null || arguments.isBlank()) {
            return@Command printToChat(context, message {
                section {
                    text("Specify one of the selected ui elements:\nbutton")
                }
            })
        }

        printToChat(context, message {
            section {
                header = "Available message interactive elements"
                controls {
                    when (arguments) {
                        "button" -> {
                            val primaryAction = PostMessageAction("ButtonPrimaryActionId", "InteractiveButtonPayloadPrimary")
                            button("Primary", primaryAction, MessageButtonStyle.PRIMARY)

                            val secondaryAction = PostMessageAction("ButtonSecondaryActionId", "InteractiveButtonPayloadSecondary")
                            button("Secondary", secondaryAction, MessageButtonStyle.SECONDARY)

                            val regularAction = PostMessageAction("ButtonRegularActionId", "InteractiveButtonPayloadRegular")
                            button("Regular", regularAction, MessageButtonStyle.REGULAR)

                            val dangerAction = PostMessageAction("ButtonDangerActionId", "InteractiveButtonPayloadDanger")
                            button("Danger", dangerAction, MessageButtonStyle.DANGER)
                        }
                    }
                }
            }
        })
    }

    val list = listOf(help, echo, interactive)

    val commands: Commands
        get() = Commands(list.map { it.toCommandDetail() })
}