package com.github.meo209.keventbus

/**
 * Annotation used to mark functions that can be registered as event handlers in the [EventBus].
 *
 * Functions annotated with `@FunctionTarget` must:
 * - Accept exactly one parameter (the event to handle).
 * - Be registered using the [EventBus.function] method.
 *
 * ### Example Usage:
 * ```kotlin
 * @FunctionTarget
 * fun handleExampleEvent(event: ExampleEvent) {
 *     println("Event received: $event")
 * }
 *
 * // Register the function as an event handler
 * EventBus.global().function(::handleExampleEvent)
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
annotation class FunctionTarget