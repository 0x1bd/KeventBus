package com.github.meo209.keventbus

import kotlin.reflect.KClass
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A synchronous event bus that allows components to communicate by publishing and subscribing to events.
 * Supports global and scoped instances, context-aware event routing, and function-based subscriptions.
 */
class EventBus private constructor() {

    companion object {
        private val globalBus = EventBus()

        /**
         * Returns the global singleton instance of the [EventBus].
         */
        fun global(): EventBus = globalBus

        /**
         * Creates and returns a new scoped instance of the [EventBus].
         */
        fun createScoped(): EventBus = EventBus()
    }

    private data class Handler<T : Event>(
        val handler: (T) -> Unit,
        val contextFilter: (T) -> Boolean
    )

    private val handlers = mutableMapOf<KClass<*>, CopyOnWriteArrayList<Handler<*>>>()

    /**
     * Registers a handler for a specific event type.
     *
     * @param eventClass The class of the event to subscribe to.
     * @param handler A lambda function that handles the event.
     * @param contextFilter A predicate that determines whether the handler should receive the event.
     */
    fun <T : Event> handler(eventClass: KClass<T>, handler: (T) -> Unit, contextFilter: (T) -> Boolean = { true }) {
        @Suppress("UNCHECKED_CAST")
        val typedHandlers = handlers.getOrPut(eventClass) { CopyOnWriteArrayList() } as CopyOnWriteArrayList<Handler<T>>
        typedHandlers.add(Handler(handler, contextFilter))
    }

    /**
     * Registers a function as an event handler.
     */
    inline fun <reified T : Event> function(noinline function: (T) -> Unit, noinline contextFilter: (T) -> Boolean = { true }) {
        handler(T::class, function, contextFilter)
    }

    /**
     * Posts an event to the event bus.
     */
    fun <T : Event> post(event: T) {
        handlers[event::class]?.forEach { handler ->
            @Suppress("UNCHECKED_CAST")
            val typedHandler = handler as Handler<T>
            if (typedHandler.contextFilter(event)) {
                typedHandler.handler(event)
            }
        }
    }
}