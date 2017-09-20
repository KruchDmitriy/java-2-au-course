package ru.spbau.mit;

import java.util.function.Function;

public interface LightFuture<T> {
    T get() throws LightExecutionException, InterruptedException;
    <R> LightFuture<R> thenApply(Function<T, R> function);
    boolean isReady();
}
