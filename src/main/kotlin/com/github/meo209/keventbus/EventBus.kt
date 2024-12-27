package com.github.meo209.keventbus

import kotlin.reflect.KClass
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap

/**
 * A synchronous event bus that allows components to communicate by publishing and subscribing to events.
 * Supports global and scoped instances, context-aware event routing, and function-based subscriptions.
 */
class EventBus private constructor(config: EventBusConfig = EventBusConfig()) {

    companion object {
        private val globalBus = EventBus()

        /**
         * Returns the global singleton instance of the [EventBus].
         */
        fun global(): EventBus = globalBus

        /**
         * Creates and returns a new scoped instance of the [EventBus].
         */
        fun createScoped(config: EventBusConfig = EventBusConfig()): EventBus = EventBus(config)
    }

    /**
     * Configuration flags for the EventBus.
     */
    enum class EventBusConfigFlag {
        ENABLE_EVENT_INHERITANCE
    }

    /**
     * Configuration class for the EventBus.
     */
    class EventBusConfig(private val flags: Set<EventBusConfigFlag> = emptySet()) {
        /**
         * Checks if a specific flag is enabled.
         */
        fun isEnabled(flag: EventBusConfigFlag): Boolean = flags.contains(flag)
    }

    private data class Handler<T : Event>(
        val handler: (T) -> Unit, val contextFilter: (T) -> Boolean
    )

    private val handlers = ConcurrentHashMap<KClass<*>, CopyOnWriteArrayList<Handler<*>>>()
    private val config: EventBusConfig

    init {
        this.config = config
    }

    /**
     * Registers a handler for a specific event type.
     *
     * @param eventClass The class of the event to subscribe to.
     * @param handler A lambda function that handles the event.
     * @param contextFilter A predicate that determines whether the handler should receive the event.
     */
    fun <T : Event> handler(eventClass: KClass<T>, handler: (T) -> Unit, contextFilter: (T) -> Boolean = { true }) {
        @Suppress("UNCHECKED_CAST") val typedHandlers =
            handlers.getOrPut(eventClass) { CopyOnWriteArrayList() } as CopyOnWriteArrayList<Handler<T>>
        typedHandlers.add(Handler(handler, contextFilter))
    }

    /**
     * Registers a function as an event handler.
     */
    inline fun <reified T : Event> function(
        noinline function: (T) -> Unit, noinline contextFilter: (T) -> Boolean = { true }
    ) {
        handler(T::class, function, contextFilter)
    }

    /**
     * Posts an event to the event bus.
     */
    fun <T : Event> post(event: T) {
        if (config.isEnabled(EventBusConfigFlag.ENABLE_EVENT_INHERITANCE)) {
            // Handle event inheritance
            postWithInheritance(event)
        } else {
            // Default behavior without inheritance
            postToHandlers(event::class, event)
        }
    }

    /**
     * Posts an event to handlers, including handlers for superclasses (if inheritance is enabled).
     */
    private fun <T : Event> postWithInheritance(event: T) {
        var currentClass: KClass<*>? = event::class
        while (currentClass != null) {
            postToHandlers(currentClass, event)
            currentClass = currentClass.supertypes.firstOrNull()?.classifier as? KClass<*>
        }
    }

    /**
     * Posts an event to handlers for a specific event class.
     */
    private fun <T : Event> postToHandlers(eventClass: KClass<*>, event: T) {
        handlers[eventClass]?.forEach { handler ->
            @Suppress("UNCHECKED_CAST") val typedHandler = handler as Handler<T>
            if (typedHandler.contextFilter(event)) {
                typedHandler.handler(event)
            }
        }
    }
}