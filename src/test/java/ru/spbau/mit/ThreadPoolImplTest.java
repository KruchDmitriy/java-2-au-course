package ru.spbau.mit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

                if (counter[0] == numThreads) {
                    counter.notifyAll();
                }

                while (counter[0] != numThreads) {
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

    @Test
    public void testTaskConsistency() throws LightExecutionException, InterruptedException {
        LightFuture[] futures = new LightFuture[numThreads];

        for (int i = 0; i < futures.length; i++) {
            int finalI = i;
            futures[i] = threadPool.submit(() -> finalI * finalI / 2);
        }

        for (int i = 0; i < futures.length; i++) {
            assertEquals(i * i / 2, futures[i].get());
        }
    }

    @Test
    public void testThenApply() throws LightExecutionException, InterruptedException {
        final int value = 10;

        Integer actual = threadPool.submit(() -> value)
                .thenApply(a -> a * a)
                .thenApply(a -> a + a).get();

        assertEquals(value * value + value * value, actual.intValue());
    }

    @Test
    public void testIsReady() throws LightExecutionException, InterruptedException {
        final boolean[] testReady = {false};
        Object syncObject = new Object();

        LightFuture future = threadPool.submit(() -> {
            synchronized (syncObject) {
                while (!testReady[0]) {
                    try {
                        syncObject.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        });

        assertFalse(future.isReady());

        synchronized (syncObject) {
            testReady[0] = true;
            syncObject.notify();
        }

        future.get();

        assertTrue(future.isReady());
    }
}