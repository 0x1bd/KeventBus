import com.github.meo209.keventbus.Event
import com.github.meo209.keventbus.EventBus
import com.github.meo209.keventbus.FunctionTarget
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBusTest {

    private var functionTargetCalled = false
    private var directTargetCalled = false
    private var scopedTargetCalled = false
    private var contextAwareTargetCalled = false

    @FunctionTarget
    private fun functionTarget(event: ExampleEvent) {
        functionTargetCalled = true
    }

    private fun functionTargetWithoutAnnotation(event: ExampleEvent3) {}

    @Test
    fun globalTest() {
        // Register a direct handler
        EventBus.global().handler(ExampleEvent::class, {
            directTargetCalled = true
        })

        // Register a function target
        EventBus.global().function<ExampleEvent>(::functionTarget)

        assertThrows<IllegalStateException> {
            EventBus.global().function<ExampleEvent3>(::functionTargetWithoutAnnotation)
        }

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

    class ExampleEvent(val shouldHandle: Boolean = true) : Event
    class ExampleEvent2 : Event
    class ExampleEvent3 : Event
}