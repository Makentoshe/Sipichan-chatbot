package com.makentoshe.schatbot

import space.jetbrains.api.runtime.types.*
import space.jetbrains.yana.userId
import java.lang.IllegalArgumentException

/**
 * This context is required for the chatbot for storing [userId] for replying.
 */
interface UserContext {
    val userId: String
}

/**
 * Factory function for fast [UserContext] creation.
 */
fun userContext(applicationPayload: ApplicationPayload) = object: UserContext {
    override val userId = applicationPayload.userId ?: throw IllegalArgumentException("Payload does not contains user id")
}

/**
 * This context is required for echo command.
 *
 * It contains [userId] for replying and current [message] from the user.
 */
data class EchoContext(override val userId: String, val message: MessageContext) : UserContext {

    companion object {

        /** Defines an echo context from payload or returns a null */
        fun from(payload: ApplicationPayload): EchoContext? = when (payload) {
            is MessagePayload -> from(payload)
            else -> null
        }

        /** Defines an echo context from [MessagePayload] */
        fun from(payload: MessagePayload): EchoContext {
            return EchoContext(payload.userId, payload.message)
        }
    }
}

/**
 * This context is required for help command.
 *
 * It contains only [userId] for replying.
 */
data class HelpContext(override val userId: String): UserContext {

    companion object {

        /** Defines a help context from payload */
        fun from(payload: ApplicationPayload): HelpContext {
            return HelpContext(payload.userId!!)
        }
    }
}

/**
 * This context is required for interactive ui command.
 *
 * It contains [userId] for replying and arguments for defining
 */
data class InteractiveContext(override val userId: String): UserContext {

    companion object {

        /** Defines an interactive context from payload */
        fun from(payload: ApplicationPayload): InteractiveContext {
            return InteractiveContext(payload.userId!!)
        }
    }
}
