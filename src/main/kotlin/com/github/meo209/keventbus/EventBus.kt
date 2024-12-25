package com.github.meo209.keventbus

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A synchronous event bus that allows components to communicate by publishing and subscribing to events.
 * Supports global and scoped instances, context-aware event routing, and function-based subscriptions.
 *
 * ### Example Usage:
 * ```kotlin
 * // Register a handler
 * EventBus.global().handler(ExampleEvent::class, { event ->
 *     println("Event received: $event")
 * })
 *
 * // Post an event
 * EventBus.global().post(ExampleEvent())
 * ```
 */
class EventBus private constructor() {

    companion object {
        private val globalBus = EventBus()

        /**
         * Returns the global singleton instance of the [EventBus].
         *
         * @return The global [EventBus] instance.
         */
        fun global(): EventBus = globalBus

        /**
         * Creates and returns a new scoped instance of the [EventBus].
         * Scoped instances are useful for modular applications where events should be isolated.
         *
         * @return A new scoped [EventBus] instance.
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
     * @param eventClass The class of the event to subscribe to (e.g., `ExampleEvent::class`).
     * @param handler A lambda function that handles the event.
     * @param contextFilter A predicate that determines whether the handler should receive the event.
     *                     Defaults to `{ true }`.
     * @param T The type of the event.
     */
    fun <T : Event> handler(eventClass: KClass<T>, handler: (T) -> Unit, contextFilter: (T) -> Boolean = { true }) {
        @Suppress("UNCHECKED_CAST")
        val typedHandlers = handlers.getOrPut(eventClass) { CopyOnWriteArrayList() } as CopyOnWriteArrayList<Handler<T>>
        typedHandlers.add(Handler(handler, contextFilter))
    }

    /**
     * Registers a function as an event handler.
     * The function must be annotated with [FunctionTarget] and accept exactly one parameter (the event).
     *
     * @param function The function to register as an event handler.
     * @param contextFilter A predicate that determines whether the function should receive the event.
     *                     Defaults to `{ true }`.
     * @param T The type of the event.
     * @throws IllegalStateException If the function does not have exactly one parameter or is not annotated with [FunctionTarget].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Event> function(function: KFunction<*>, contextFilter: (T) -> Boolean = { true }) {
        if (function.parameters.size != 1)
            throw IllegalStateException("Function doesn't have the required event as a parameter.")
        val eventClass = function.parameters.last().type.classifier as? KClass<T> ?: return
        if (Event::class.java.isAssignableFrom(eventClass.java)) {
            if (function.hasAnnotation<FunctionTarget>()) {
                function.isAccessible = true
                val handler = { event: Event -> function.call(event) }
                handler(eventClass, handler as (T) -> Unit, contextFilter)
            } else
                throw IllegalStateException("Event function needs @FunctionTarget annotation.")
        }
    }

    /**
     * Posts an event to the event bus.
     * All registered handlers for the event type will be invoked if their [contextFilter] conditions are satisfied.
     *
     * @param event The event to publish.
     * @param T The type of the event.
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