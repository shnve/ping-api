package fr.shom.ping.exception;

import java.util.function.Consumer;

public final class ThrowingConsumerWrapper {
    public static <T> Consumer<T> throwingConsumerWrapper(ThrowingConsumer<T, Exception> throwingConsumer) {
        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
}
