package ru.spbau.mit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

public class ThreadPoolImplTest {
    private final static int numThreads = 100;
    private ThreadPoolImpl threadPool;

    @Before
    public void setUp() {
        threadPool = new ThreadPoolImpl(numThreads);
    }

    @After
    public void tearDown() {
        threadPool.shutdown();
    }

    @Test
    public void testNumThreads() throws LightExecutionException, InterruptedException {
        final Integer[] counter = {0};

        Supplier<Void> supplier = () -> {
            synchronized (counter) {
                counter[0]++;

                if (counter[0] == numThreads + 1) {
                    counter.notifyAll();
                }

                while (counter[0] != numThreads + 1) {
                    try {
                        counter.wait();
                    } catch (InterruptedException ignored) {}
                }
            }

            return null;
        };

        LightFuture[] futures = new LightFuture[numThreads];

        for (int i = 0; i < numThreads; i++) {
            futures[i] = threadPool.submit(supplier);
        }

        for (LightFuture future: futures) {
            future.get();
        }
    }

    @Test(expected = LightExecutionException.class)
    public void testThrow() throws LightExecutionException, InterruptedException {
        LightFuture<Void> future = threadPool.submit(() -> {
            throw new RuntimeException();
        });

        future.get();
    }
}