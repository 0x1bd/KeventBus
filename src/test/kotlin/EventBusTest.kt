import com.kvxd.eventbus.Event
import com.kvxd.eventbus.EventBus
import com.kvxd.eventbus.EventPriority
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventBusTest {

    data class TestEvent(val message: String) : Event
    data class MutableTestEvent(var message: String) : Event

    @Test
    fun `test concise handler registration`() {
        val eventBus = EventBus.create()
        val event = TestEvent("Concise Handler Test")
        var receivedMessage: String? = null

        eventBus.handler(TestEvent::class) { receivedEvent ->
            receivedMessage = receivedEvent.message
        }

        eventBus.post(event)

        assertEquals("Concise Handler Test", receivedMessage)
    }

    @Test
    fun `test handler disable`() {
        val eventBus = EventBus.create()
        val event = TestEvent("Handler Disable Test")
        var receivedMessage: String? = null

        val handler = eventBus.handler(TestEvent::class) { receivedEvent ->
            receivedMessage = receivedEvent.message
        }

        eventBus.post(event)
        assertEquals("Handler Disable Test", receivedMessage)

        handler.disable()

        val event2 = TestEvent("This should not be handled")
        eventBus.post(event2)
        assertEquals("Handler Disable Test", receivedMessage)
    }

    @Test
    fun `test handler enable`() {
        val eventBus = EventBus.create()
        val event = TestEvent("Handler Enable Test")
        var receivedMessage: String? = null

        val handler = eventBus.handler(TestEvent::class) { receivedEvent ->
            receivedMessage = receivedEvent.message
        }
        handler.disable()

        eventBus.post(event)
        assertEquals(null, receivedMessage)

        handler.enable()

        val event2 = TestEvent("This should be handled")
        eventBus.post(event2)
        assertEquals("This should be handled", receivedMessage)
    }

    @Test
    fun `test multiple handlers for same event type`() {
        val eventBus = EventBus.create()
        val event = TestEvent("Multiple Handlers Test")
        val receivedMessages = mutableListOf<String>()

        eventBus.handler(TestEvent::class) { receivedEvent ->
            receivedMessages.add("Handler 1: ${receivedEvent.message}")
        }
        eventBus.handler(TestEvent::class) { receivedEvent ->
            receivedMessages.add("Handler 2: ${receivedEvent.message}")
        }

        eventBus.post(event)

        assertEquals(2, receivedMessages.size)
        assertTrue(receivedMessages.contains("Handler 1: Multiple Handlers Test"))
        assertTrue(receivedMessages.contains("Handler 2: Multiple Handlers Test"))
    }

    @Test
    fun `test forwarding between buses`() {
        val eventBus1 = EventBus.create()
        val eventBus2 = EventBus.create()
        val event = TestEvent("Forwarding Test")
        var receivedMessage: String? = null

        // Set up forwarding
        eventBus1.forward(eventBus2)

        // Register handler on the second bus
        eventBus2.handler(TestEvent::class) { receivedEvent ->
            receivedMessage = receivedEvent.message
        }

        // Post event to the first bus
        eventBus1.post(event)

        assertEquals("Forwarding Test", receivedMessage)

        // Stop forwarding and test again
        eventBus1.stopForwarding(eventBus2)
        val event2 = TestEvent("This should not be forwarded")
        eventBus1.post(event2)
        assertEquals("Forwarding Test", receivedMessage) // Should not change
    }

    @Test
    fun `test stop forwarding all`() {
        val eventBus1 = EventBus.create()
        val eventBus2 = EventBus.create()
        val eventBus3 = EventBus.create()
        var receivedCount = 0

        // Set up forwarding to multiple buses
        eventBus1.forward(eventBus2).forward(eventBus3)

        // Register handlers
        eventBus2.handler(TestEvent::class) { receivedCount++ }
        eventBus3.handler(TestEvent::class) { receivedCount++ }

        // Post event
        eventBus1.post(TestEvent("First event"))
        assertEquals(2, receivedCount)

        // Stop all forwarding
        eventBus1.stopForwardingAll()

        // Post another event
        eventBus1.post(TestEvent("Second event"))
        assertEquals(2, receivedCount) // Should not change
    }

    @Test
    fun `test handler priority`() {
        val eventBus = EventBus.create()
        val event = TestEvent("Handler Priority Test")
        val receivedMessages = mutableListOf<String>()

        eventBus.handler(TestEvent::class, priority = EventPriority.LOW) { receivedEvent ->
            receivedMessages.add("Low Priority Handler: ${receivedEvent.message}")
        }
        eventBus.handler(TestEvent::class, priority = EventPriority.HIGH) { receivedEvent ->
            receivedMessages.add("High Priority Handler: ${receivedEvent.message}")
        }
        eventBus.handler(TestEvent::class) { receivedEvent ->
            receivedMessages.add("Normal Priority Handler: ${receivedEvent.message}")
        }

        eventBus.post(event)

        assertEquals(3, receivedMessages.size)
        assertEquals("High Priority Handler: Handler Priority Test", receivedMessages[0])
        assertEquals("Normal Priority Handler: Handler Priority Test", receivedMessages[1])
        assertEquals("Low Priority Handler: Handler Priority Test", receivedMessages[2])
    }

    var receivedMessage: String? = null

    fun testFunction(event: TestEvent) {
        receivedMessage = event.message
    }

    @Test
    fun `test function method`() {
        val eventBus = EventBus.create()
        val event = TestEvent("Function Method Test")

        // Register a handler using the function method
        val handler = eventBus.function<TestEvent>(::testFunction)

        // Post the event and verify the handler is called
        eventBus.post(event)
        assertEquals("Function Method Test", receivedMessage)

        // Disable the handler and post another event
        handler.disable()
        val event2 = TestEvent("This should not be handled")
        eventBus.post(event2)
        assertEquals("Function Method Test", receivedMessage) // Should not change

        // Enable the handler and post another event
        handler.enable()
        val event3 = TestEvent("This should be handled now")
        eventBus.post(event3)
        assertEquals("This should be handled now", receivedMessage)
    }

    @Test
    fun `test event filtering`() {
        val eventBus = EventBus.create()
        val event1 = TestEvent("This should be handled")
        val event2 = TestEvent("This should not be handled")
        var receivedMessage: String? = null

        // Register a handler with a filter
        eventBus.handler(TestEvent::class, filter = { event -> event.message == "This should be handled" }) { receivedEvent ->
            receivedMessage = receivedEvent.message
        }

        // Post the first event (should be handled)
        eventBus.post(event1)
        assertEquals("This should be handled", receivedMessage)

        // Post the second event (should not be handled)
        eventBus.post(event2)
        assertEquals("This should be handled", receivedMessage) // Should not change
    }

    @Test
    fun `test event modification`() {
        val eventBus = EventBus.create()
        val event = MutableTestEvent("Not modified")

        // Register a handler with a filter
        eventBus.handler(MutableTestEvent::class) { receivedEvent ->
            receivedEvent.message = "Modified"
        }

        // Post the first event (should be handled)
        eventBus.post(event)
        assertEquals("Modified", event.message)
    }
}