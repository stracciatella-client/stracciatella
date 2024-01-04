package net.stracciatella.internal.unsafe;

import java.io.PrintStream;

import net.stracciatella.internal.util.NullOutputStream;
import net.stracciatella.internal.module.StracciatellaThrowables;
import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.datamodel.ModelVM;
import sun.misc.Unsafe;

/**
 * We assume java 21 HotSpot and 64 bit.
 */
public class UnsafeHelper {

    private static final Unsafe UNSAFE;
    private static final DataModel MODEL;
    private static final long CLASS_OFFSET;
    private static final boolean EIGHT_BYTE_CLASS;

    static {
        System.setProperty("jol.skipInstallAttach", "true");
        System.setProperty("jol.skipDynamicAttach", "true");
        System.setProperty("jol.skipHotspotSAAttach", "true");
        try {
            var f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);

            final var oldOut = System.out;
            System.setOut(new PrintStream(NullOutputStream.INSTANCE));
            MODEL = new ModelVM();
            CLASS_OFFSET = MODEL.markHeaderSize();
            EIGHT_BYTE_CLASS = MODEL.classHeaderSize() == 8;
            System.setOut(oldOut);
        } catch (Throwable t) {
            throw StracciatellaThrowables.propagate(t);
        }
    }

    public static <B> B unsafeCast(Object object, Class<B> clazz) {
        return unsafeCast(object, klass(clazz));
    }

    @SuppressWarnings("unchecked")
    public static <B> B unsafeCast(Object object, long klass) {
        if (EIGHT_BYTE_CLASS) {
            UNSAFE.getAndSetLong(object, CLASS_OFFSET, klass);
        } else {
            UNSAFE.getAndSetInt(object, CLASS_OFFSET, (int) klass);
        }
        return (B) object;
    }

    public static long klass(Class<?> clazz) {
        try {
            return klass(UNSAFE.allocateInstance(clazz));
        } catch (InstantiationException e) {
            throw StracciatellaThrowables.propagate(e);
        }
    }

    private static long klass(Object instance) {
        if (EIGHT_BYTE_CLASS) return UNSAFE.getLong(instance, CLASS_OFFSET);
        return UNSAFE.getInt(instance, CLASS_OFFSET);
    }

    public static Unsafe unsafe() {
        return UNSAFE;
    }
}
