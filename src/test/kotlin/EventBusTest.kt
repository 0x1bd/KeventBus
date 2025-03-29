import com.kvxd.eventbus.Event
import com.kvxd.eventbus.EventBus
import com.kvxd.eventbus.EventPriority
import com.kvxd.eventbus.handler
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.reflect.KClass

class EventBusTest {

    // Test event classes
    class TestEvent : Event
    class AnotherTestEvent : Event

    @Test
    fun testCreateEventBus() {
        val eventBus = EventBus()
        assertNotNull(eventBus, "EventBus instance should not be null")
    }

    @Test
    fun testHandlerRegistration() = runTest {
        val eventBus = EventBus()
        var eventHandled = false

        eventBus.handler(TestEvent::class) { eventHandled = true }
        eventBus.post(TestEvent())

        assertTrue(eventHandled, "Event handler should be called when event is posted")
    }

    @Test
    fun testHandlerPriority() = runTest {
        val eventBus = EventBus()
        val handledEvents = mutableListOf<Int>()

        eventBus.handler(TestEvent::class, priority = EventPriority.LOW) { handledEvents.add(1) }
        eventBus.handler(TestEvent::class, priority = EventPriority.HIGH) { handledEvents.add(3) }
        eventBus.handler(TestEvent::class, priority = EventPriority.NORMAL) { handledEvents.add(2) }

        eventBus.post(TestEvent())

        assertEquals(listOf(3, 2, 1), handledEvents, "Handlers should be executed in order of priority")
    }

    @Test
    fun testHandlerFilter() = runTest {
        val eventBus = EventBus()
        var eventHandled = false

        eventBus.handler(TestEvent::class, filter = { false }) { eventHandled = true }
        eventBus.post(TestEvent())

        assertFalse(eventHandled, "Event handler should not be called if filter returns false")
    }

    @Test
    fun testHandlerEnableDisable() = runTest {
        val eventBus = EventBus()
        var eventHandled = false

        val handler = eventBus.handler(TestEvent::class) { eventHandled = true }
        handler.disable()
        eventBus.post(TestEvent())

        assertFalse(eventHandled, "Event handler should not be called when disabled")

        handler.enable()
        eventBus.post(TestEvent())

        assertTrue(eventHandled, "Event handler should be called when enabled")
    }

    @Test
    fun testEventForwarding() = runTest {
        val mainBus = EventBus()
        val forwardedBus = EventBus()
        var eventHandled = false

        mainBus.forward(forwardedBus)
        forwardedBus.handler(TestEvent::class) { eventHandled = true }

        mainBus.post(TestEvent())

        assertTrue(eventHandled, "Event should be forwarded to the second bus and handled")
    }

    @Test
    fun testEventForwardingWithFilter() = runTest {
        val mainBus = EventBus()
        val forwardedBus = EventBus()
        var eventHandled = false

        mainBus.forward(forwardedBus) { it is TestEvent }
        forwardedBus.handler(TestEvent::class) { eventHandled = true }

        mainBus.post(TestEvent())
        assertTrue(eventHandled, "TestEvent should be forwarded and handled")

        eventHandled = false
        mainBus.post(AnotherTestEvent())
        assertFalse(eventHandled, "AnotherTestEvent should not be forwarded")
    }

    @Test
    fun testStopForwarding() = runTest {
        val mainBus = EventBus()
        val forwardedBus = EventBus()
        var eventHandled = false

        mainBus.forward(forwardedBus)
        forwardedBus.handler(TestEvent::class) { eventHandled = true }

        mainBus.stopForwarding(forwardedBus)
        mainBus.post(TestEvent())

        assertFalse(eventHandled, "Event should not be forwarded after stopping forwarding")
    }

    @Test
    fun testStopForwardingAll() = runTest {
        val mainBus = EventBus()
        val forwardedBus1 = EventBus()
        val forwardedBus2 = EventBus()
        val forwardedBus3 = EventBus()
        var eventHandled1 = false
        var eventHandled2 = false
        var eventHandled3 = true

        mainBus.forward(forwardedBus1)
        mainBus.forward(forwardedBus2)
        forwardedBus2.forward(forwardedBus3)
        forwardedBus1.handler(TestEvent::class) { eventHandled1 = true }
        forwardedBus2.handler(TestEvent::class) { eventHandled2 = true }
        forwardedBus3.handler(TestEvent::class, filter = { false }) { eventHandled3 = false }

        mainBus.stopForwardingAll()
        mainBus.post(TestEvent())

        assertFalse(eventHandled1, "Event should not be forwarded to bus1 after stopping all forwarding")
        assertFalse(eventHandled2, "Event should not be forwarded to bus2 after stopping all forwarding")
        assertTrue(eventHandled3, "Event should not be forwarded to bus2 after stopping all forwarding")
    }
}