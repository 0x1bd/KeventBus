import com.github.meo209.keventbus.Event
import com.github.meo209.keventbus.EventBus
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBusTest {

    private var functionTargetCalled = false
    private var directTargetCalled = false
    private var scopedTargetCalled = false
    private var contextAwareTargetCalled = false
    private var inheritanceTargetCalled = false
    private var asyncTargetCalled = false
    private var filteredEventHandled = false
    private var tracedEventHandled = false

    private fun functionTarget(event: ExampleEvent) {
        functionTargetCalled = true
    }

    @Test
    fun globalTest() {
        // Register a direct handler
        EventBus.global().handler(ExampleEvent::class, {
            directTargetCalled = true
        })

        // Register a function target
        EventBus.global().function(::functionTarget)

        // Post an event and check if both handlers are called
        EventBus.global().post(ExampleEvent())

        // Assert that both handlers were called
        assertEquals(true, functionTargetCalled, "FunctionTarget should have been called")
        assertEquals(true, directTargetCalled, "DirectTarget should have been called")
    }

    @Test
    fun scopeTest() {
        // Create a scoped event bus
        val scope = EventBus.createScoped()

        // Register a handler in the scoped event bus
        scope.handler(ExampleEvent2::class, { event ->
            scopedTargetCalled = true
        })

        // Post an event to the global event bus (should not trigger the scoped handler)
        EventBus.global().post(ExampleEvent2())

        // Assert that the scoped handler was not called
        assertEquals(false, scopedTargetCalled, "Scoped handler should not have been called")

        // Post an event to the scoped event bus
        scope.post(ExampleEvent2())

        // Assert that the scoped handler was called
        assertEquals(true, scopedTargetCalled, "Scoped handler should have been called")
    }

    @Test
    fun contextAwareTest() {
        // Register a context-aware handler
        EventBus.global().handler(ExampleEvent::class, { event ->
            contextAwareTargetCalled = true
        }, { event -> event.shouldHandle })

        // Post an event that should not trigger the handler
        EventBus.global().post(ExampleEvent(shouldHandle = false))

        // Assert that the handler was not called
        assertEquals(false, contextAwareTargetCalled, "Context-aware handler should not have been called")

        // Post an event that should trigger the handler
        EventBus.global().post(ExampleEvent(shouldHandle = true))

        // Assert that the handler was called
        assertEquals(true, contextAwareTargetCalled, "Context-aware handler should have been called")
    }

    @Test
    fun inheritanceTest() {
        // Create an EventBus with event inheritance enabled
        val eventBus = EventBus.createScoped(EventBus.EventBusConfig(setOf(EventBus.EventBusConfigFlag.ENABLE_EVENT_INHERITANCE)))

        // Register a handler for the base event
        eventBus.handler(BaseEvent::class, { event ->
            inheritanceTargetCalled = true
        })

        // Post a subclass event
        eventBus.post(SpecificEvent())

        // Assert that the handler for the base event was called
        assertEquals(true, inheritanceTargetCalled, "Handler for BaseEvent should have been called")
    }

    @Test
    fun asyncProcessingTest() {
        // Create an EventBus with async processing enabled
        val eventBus = EventBus.createScoped(EventBus.EventBusConfig(setOf(EventBus.EventBusConfigFlag.ENABLE_ASYNC_PROCESSING)))

        // Register a handler
        eventBus.handler(ExampleEvent::class, { event ->
            asyncTargetCalled = true
        })

        // Post an event
        eventBus.post(ExampleEvent())

        // Wait for the async processing to complete
        Thread.sleep(100)

        // Assert that the handler was called
        assertEquals(true, asyncTargetCalled, "Handler should have been called asynchronously")
    }

    @Test
    fun eventFilteringTest() {
        // Create an EventBus with event filtering enabled
        val eventBus = EventBus.createScoped(EventBus.EventBusConfig(setOf(EventBus.EventBusConfigFlag.ENABLE_EVENT_FILTERING)))

        // Register a handler
        eventBus.handler(ExampleEvent::class, { event ->
            filteredEventHandled = true
        }, { it.shouldHandle == true } )

        // Post an event that should be filtered out
        eventBus.post(ExampleEvent(shouldHandle = false))

        // Assert that the handler was not called
        assertEquals(false, filteredEventHandled, "Handler should not have been called for filtered event")
    }

    @Test
    fun eventTracingTest() {
        // Create an EventBus with event tracing enabled
        val eventBus = EventBus.createScoped(EventBus.EventBusConfig(setOf(EventBus.EventBusConfigFlag.ENABLE_EVENT_TRACING)))

        // Register a handler
        eventBus.handler(ExampleEvent::class, { event ->
            tracedEventHandled = true
        })

        // Post an event
        eventBus.post(ExampleEvent())

        // Assert that the handler was called
        assertEquals(true, tracedEventHandled, "Handler should have been called with tracing enabled")
    }

    class ExampleEvent(val shouldHandle: Boolean = true) : Event
    class ExampleEvent2 : Event

    // Define an event hierarchy for inheritance testing
    open class BaseEvent : Event
    class SpecificEvent : BaseEvent()
}