package ru.spbau.mit.threadpool;

import java.util.function.Supplier;

public interface ThreadPool {
    <T> LightFuture<T> submit(Supplier<T> task) throws RejectedExecutionException;
    void shutdown();
    int getNumThreads();
    boolean isShutdown();
}
