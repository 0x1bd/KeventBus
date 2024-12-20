package com.github.meo209.keventbus

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible
import java.util.concurrent.CopyOnWriteArrayList

class EventBus private constructor() {

    companion object {
        private val globalBus = EventBus()

        fun global(): EventBus = globalBus

        fun createScoped(): EventBus = EventBus()
    }

    private val handlers = mutableMapOf<KClass<*>, CopyOnWriteArrayList<(Event) -> Unit>>()

    fun <T : Event> handler(eventClass: KClass<T>, handler: (T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val typedHandlers =
            handlers.getOrPut(eventClass) { CopyOnWriteArrayList() } as CopyOnWriteArrayList<(T) -> Unit>

        typedHandlers.add(handler)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Event> function(function: KFunction<*>) {
        if (function.parameters.size != 1)
            throw IllegalStateException("Function doesnt have the required event as a parameter.")
        val eventClass = function.parameters.last().type.classifier as? KClass<T> ?: return
        if (Event::class.java.isAssignableFrom(eventClass.java)) {
            if (function.hasAnnotation<FunctionTarget>()) {
                function.isAccessible = true
                val handler = { event: Event -> function.call(event) }
                handler(eventClass, handler as (T) -> Unit)
            } else
                throw IllegalStateException("Event function needs @FunctionTarget annotation.")
        }
    }

    fun <T : Event> post(event: T) {
        handlers[event::class]?.forEach { it(event) }
    }

}