package net.stracciatella.module;

import java.util.function.Function;

import org.jetbrains.annotations.Contract;

public class StracciatellaThrowables {
    @Contract("_->fail")
    public static Error propagate(Throwable t) {
        throw propagate(t, Error.class, Error::new);
    }

    @Contract("_,_,_->fail")
    public static <T extends Error> T propagate(Throwable t, Class<T> cls, Function<Throwable, T> mapper) {
        if (cls.isInstance(t)) throw cls.cast(t);
        var e = mapper.apply(t);
        e.setStackTrace(new StackTraceElement[0]);
        throw e;
    }
}
