package com.kvxd.eventbus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * A synchronous event bus that allows components to communicate by publishing and subscribing to events.
 */
open class EventBus{

    /**
     * Represents an event handler with its associated properties.
     *
     * @property handler The function that handles the event.
     * @property isEnabled Whether the handler is currently enabled.
     * @property priority The priority of the handler (higher priority handlers are executed first).
     * @property filter A predicate that determines whether the handler should process the event.
     */
    data class Handler<T : Event>(
        val handler: suspend (T) -> Unit,  // Changed to suspend function
        var isEnabled: Boolean = true,
        val priority: EventPriority = EventPriority.NORMAL,
        val filter: (T) -> Boolean = { true }
    ) {
        fun disable() {
            isEnabled = false
        }

        fun enable() {
            isEnabled = true
        }
    }

    private val handlers = ConcurrentHashMap<KClass<*>, CopyOnWriteArrayList<Handler<*>>>()
    private val forwardedBuses = mutableSetOf<Pair<EventBus, (Event) -> Boolean>>()
    var onError: (Throwable) -> Unit = {}  // Global error handler

    /**
     * Registers a handler for a specific event type.
     *
     * @param eventClass The class of the event to subscribe to.
     * @param priority The priority of the handler (default is [EventPriority.NORMAL]).
     * @param filter A predicate that determines whether the handler should process the event (default allows all events).
     * @param handler The function that handles the event.
     * @return The registered handler, which can be disabled or enabled later.
     */
    fun <T : Event> handler(
        eventClass: KClass<T>,
        priority: EventPriority = EventPriority.NORMAL,
        filter: (T) -> Boolean = { true },
        handler: suspend (T) -> Unit
    ): Handler<T> {
        @Suppress("UNCHECKED_CAST")
        val typedHandlers = handlers.getOrPut(eventClass) { CopyOnWriteArrayList() } as CopyOnWriteArrayList<Handler<T>>
        val newHandler = Handler(handler, priority = priority, filter = filter)
        typedHandlers.add(newHandler)
        typedHandlers.sortByDescending { it.priority }
        return newHandler
    }

    /**
     * Registers a function as an event handler.
     *
     * @param function The function that handles the event.
     * @return The registered handler.
     */
    inline fun <reified T : Event> function(noinline function: (T) -> Unit): Handler<T> {
        return handler(T::class, handler = function)
    }

    /**
     * Posts an event to the event bus.
     *
     * @param event The event to post.
     */
    suspend fun <T : Event> post(event: T) {
        // Handle local handlers
        handlers[event::class]?.forEach { handler ->
            @Suppress("UNCHECKED_CAST") val typedHandler = handler as Handler<T>
            if (typedHandler.isEnabled && typedHandler.filter(event)) {
                try {
                    typedHandler.handler(event)
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }

        // Forward to all forwarded buses if the filter allows
        forwardedBuses.forEach { (bus, filter) ->
            if (filter(event)) {
                bus.post(event)
            }
        }
    }

    /**
     * Forwards all events from this bus to another bus, filtered by the provided predicate.
     *
     * @param targetBus The event bus to forward events to.
     * @param filter A predicate that determines whether an event should be forwarded (default allows all events).
     * @return This event bus instance for method chaining.
     */
    fun forward(targetBus: EventBus, filter: (Event) -> Boolean = { true }): EventBus {
        forwardedBuses.add(targetBus to filter)
        return targetBus
    }

    /**
     * Forwards all events from this bus to another bus, filtered by the provided predicate.
     *
     * @param filter A predicate that determines whether an event should be forwarded (default allows all events).
     * @return This event bus instance for method chaining.
     */
    fun forward(filter: (Event) -> Boolean = { true }): EventBus {
        val bus = EventBus()
        forward(bus, filter)
        return bus
    }

    /**
     * Forwards all events from this bus to another bus.
     *
     * @return This event bus instance for method chaining.
     */
    fun forward(scope: CoroutineScope): EventBus {
        val bus = EventBus()
        forward(bus)
        return bus
    }

    /**
     * Stops forwarding events to a specific bus.
     *
     * @param targetBus The event bus to stop forwarding events to.
     * @return This event bus instance for method chaining.
     */
    fun stopForwarding(targetBus: EventBus): EventBus {
        forwardedBuses.removeAll { it.first == targetBus }
        return this
    }

    /**
     * Stops forwarding events to all buses.
     *
     * @return This event bus instance for method chaining.
     */
    fun stopForwardingAll(): EventBus {
        forwardedBuses.clear()
        return this
    }
}

/**
 * Extension function for concise handler registration.
 *
 * @param priority The priority of the handler (default is [EventPriority.NORMAL]).
 * @param filter A predicate that determines whether the handler should process the event (default allows all events).
 * @param handler The function that handles the event.
 * @return The registered handler.
 */
inline fun <reified T : Event> EventBus.handler(
    priority: EventPriority = EventPriority.NORMAL,
    noinline filter: (T) -> Boolean = { true },
    noinline handler: suspend (T) -> Unit
): EventBus.Handler<T> {
    return this.handler(T::class, priority, filter, handler)
}