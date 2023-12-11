package net.stracciatella.module;

import java.lang.ref.Reference;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MemoryLeakDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger("MemoryLeakDetector");

    static void start(SimpleModuleManager manager, String name, Queue<Reference<Class<?>>> loaded) {
        var thread = Thread.ofPlatform().daemon(false).name("MemoryLeakDetector-" + name).start(() -> {
            try {
                Reference<Class<?>> reference;
                for (int i = 0; i < 10; i++) {
                    boolean failed = false;
                    while ((reference = loaded.peek()) != null) {
                        if (reference.get() != null) {
                            failed = true;
                            break;
                        }
                        loaded.poll();
                    }
                    if (!failed) {
                        return;
                    }
                    Runtime.getRuntime().gc();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw StracciatellaThrowables.propagate(e);
                    }
                }
                LOGGER.error("Probable Memory leak in module " + name);
                while ((reference = loaded.poll()) != null) {
                    var cls = reference.get();
                    if (cls == null) continue;
                    LOGGER.error(" - " + cls.getName() + " (" + Integer.toHexString(cls.hashCode()) + ")");
                }
            } finally {
                manager.workers().remove(Thread.currentThread());
            }
        });
        manager.workers().add(thread);
    }
}
