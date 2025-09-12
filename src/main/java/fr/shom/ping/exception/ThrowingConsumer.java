package fr.shom.ping.exception;

@FunctionalInterface
public interface ThrowingConsumer <T, E extends Exception> {
    void accept(T t) throws E;
}
