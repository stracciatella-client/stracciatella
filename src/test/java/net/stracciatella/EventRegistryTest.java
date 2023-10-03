package net.stracciatella;

import static org.junit.jupiter.api.Assertions.*;

import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.event.EventFactory;
import org.junit.jupiter.api.Test;

public class EventRegistryTest {

    @Test
    void testEvents() {
        final var testEvent = EventFactory.createArrayBacked(TestCallback.class, testCallbacks -> (i1, o1, b1, i2) -> {
            int max = 0;
            for (TestCallback call : testCallbacks) {
                max = Math.max(call.callback(i1, o1, b1, i2), max);
            }
            return max;
        });
        EventRegistry.register(testEvent, (i1, o1, b1, i2) -> Math.max(i1, i2));
        assertEquals(testEvent.invoker().callback(1, null, true, 2), 2);

        final var event = EntityElytraEvents.ALLOW;

        var listener = (EntityElytraEvents.Allow) entity -> true;
        EventRegistry.register(event, listener);
        assertTrue(event.invoker().allowElytraFlight(null));
        var listener2 = (EntityElytraEvents.Allow) entity -> false;
        EventRegistry.register(event, listener2);
        assertFalse(event.invoker().allowElytraFlight(null));
        EventRegistry.unregister(event, listener);
        assertFalse(event.invoker().allowElytraFlight(null));
        EventRegistry.unregister(event, listener2);
        assertTrue(event.invoker().allowElytraFlight(null));
        EventRegistry.register(event, listener);
        assertTrue(event.invoker().allowElytraFlight(null));
        var listener3 = (EntityElytraEvents.Allow) entity -> {
            throw new IllegalStateException();
        };
        EventRegistry.register(event, listener3);
        assertThrows(IllegalStateException.class, () -> event.invoker().allowElytraFlight(null));
    }

    public interface TestCallback {
        int callback(int i1, Object o1, boolean b1, int i2);
    }
}
