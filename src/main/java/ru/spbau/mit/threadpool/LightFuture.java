package ru.spbau.mit.threadpool;

import java.util.function.Function;

public interface LightFuture<T> {
    T get() throws LightExecutionException, InterruptedException;
    <R> LightFuture<R> thenApply(Function<T, R> function) throws RejectedExecutionException;
    boolean isReady();
}
