package com.github.meo209.keventbus

/**
 * Marker interface for events that can be published and subscribed to via the [EventBus].
 *
 * Any class implementing this interface can be used as an event in the [EventBus] system.
 *
 * ### Example Usage:
 * ```kotlin
 * class ExampleEvent : Event {
 *     val data: String = "Example"
 * }
 * ```
 */
interface Event