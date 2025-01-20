package com.kvxd.eventbus

import kotlin.reflect.KClass
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap

/**
 * A synchronous event bus that allows components to communicate by publishing and subscribing to events.
 */
class EventBus private constructor() {

    companion object {
        /**
         * Creates and returns a new instance of the [EventBus].
         */
        fun createScoped(): EventBus = EventBus()
    }

    data class Handler<T : Event>(
        val handler: (T) -> Unit,
        var isEnabled: Boolean = true,
        val priority: EventPriority = EventPriority.NORMAL
    ) {
        /**
         * Disables this com.github.meo209.keventbus.handler.
         */
        fun disable() {
            isEnabled = false
        }

        /**
         * Enables this com.github.meo209.keventbus.handler.
         */
        fun enable() {
            isEnabled = true
        }
    }

    private val handlers = ConcurrentHashMap<KClass<*>, CopyOnWriteArrayList<Handler<*>>>()
    private val forwardedBuses = mutableSetOf<EventBus>()

    /**
     * Registers a com.github.meo209.keventbus.handler for a specific event type.
     *
     * @param eventClass The class of the event to subscribe to.
     * @param handler A lambda function that handles the event.
     * @param priority The priority of the com.github.meo209.keventbus.handler.
     * @return The registered com.github.meo209.keventbus.handler, which can be disabled or enabled later.
     */
    fun <T : Event> handler(
        eventClass: KClass<T>,
        priority: EventPriority = EventPriority.NORMAL,
        handler: (T) -> Unit
    ): Handler<T> {
        @Suppress("UNCHECKED_CAST") val typedHandlers =
            handlers.getOrPut(eventClass) { CopyOnWriteArrayList() } as CopyOnWriteArrayList<Handler<T>>
        val newHandler = Handler(handler, priority = priority)
        typedHandlers.add(newHandler)
        // Sort handlers by priority (highest first)
        typedHandlers.sortByDescending { it.priority }
        return newHandler
    }

    /**
     * Registers a function as an event com.github.meo209.keventbus.handler.
     */
    inline fun <reified T : Event> function(noinline function: (T) -> Unit): Handler<T> {
        return handler(T::class, handler = function)
    }

    /**
     * Posts an event to the event bus.
     */
    fun <T : Event> post(event: T) {
        // Handle local handlers
        handlers[event::class]?.forEach { handler ->
            @Suppress("UNCHECKED_CAST") val typedHandler = handler as Handler<T>
            if (typedHandler.isEnabled) {
                typedHandler.handler(event)
            }
        }

        // Forward to all forwarded buses
        forwardedBuses.forEach { it.post(event) }
    }

    /**
     * Forwards all events from this bus to another bus.
     */
    fun forward(targetBus: EventBus): EventBus {
        forwardedBuses.add(targetBus)
        return this
    }

    /**
     * Stops forwarding events to a specific bus.
     */
    fun stopForwarding(targetBus: EventBus): EventBus {
        forwardedBuses.remove(targetBus)
        return this
    }

    /**
     * Stops forwarding events to all buses.
     */
    fun stopForwardingAll(): EventBus {
        forwardedBuses.clear()
        return this
    }
}

/**
 * Extension function for concise com.github.meo209.keventbus.handler registration.
 */
inline fun <reified T : Event> EventBus.handler(
    priority: EventPriority = EventPriority.NORMAL,
    noinline handler: (T) -> Unit
): EventBus.Handler<T> {
    return this.handler(T::class, priority, handler)
}