package org.tillerino.jagger.processor.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Supplier;
import org.apache.commons.lang3.exception.ContextedRuntimeException;

public class Exceptions {
    public static <T> T getWithContext(String label, Object value, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ContextedRuntimeException e) {
            throw e.addContextValue(label, value);
        }
    }

    public static void runWithContext(String label, Object value, Runnable runnable) {
        try {
            runnable.run();
        } catch (ContextedRuntimeException e) {
            throw e.addContextValue(label, value);
        }
    }

    public static <T> @Nonnull T notNull(@Nullable T t, String message, Object... args) {
        if (t == null) {
            throw new ContextedRuntimeException(message.formatted(args));
        }
        return t;
    }

    public static <T> @Nonnull T notNull(@Nullable T t, String message) {
        if (t == null) {
            throw new ContextedRuntimeException(message);
        }
        return t;
    }

    public static <T> @Nonnull T notNull(@Nullable T t) {
        if (t == null) {
            throw new ContextedRuntimeException("null");
        }
        return t;
    }

    public static ContextedRuntimeException unexpected() {
        return new ContextedRuntimeException("This is so unexpected that nobody bothered to write an error message. "
                + "Or maybe this feature is incomplete. Either way, kindly report this. Thank you.");
    }
}
