package ru.spbau.mit.threadpool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThreadPoolImplTest {
    private static final int NUM_THREADS = 1000;
    private ThreadPoolImpl threadPool;

    @Before
    public void setUp() {
        threadPool = new ThreadPoolImpl(NUM_THREADS);
    }

    @After
    public void tearDown() {
        threadPool.shutdown();
    }

    @Test
    public void testNumThreads() throws LightExecutionException,
            InterruptedException,
            RejectedExecutionException {
        final Integer[] counter = {0};

        Supplier<Void> supplier = () -> {
            synchronized (counter) {
                counter[0]++;

                if (counter[0] == NUM_THREADS) {
                    counter.notifyAll();
                }

                while (counter[0] != NUM_THREADS) {
                    try {
                        counter.wait();
                    } catch (InterruptedException ignored) { }
                }
            }

            return null;
        };

        LightFuture[] futures = new LightFuture[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++) {
            futures[i] = threadPool.submit(supplier);
        }

        for (LightFuture future: futures) {
            future.get();
        }

        assertEquals(NUM_THREADS, threadPool.getNumThreads());
    }

    @Test
    public void testThrow() throws LightExecutionException,
            InterruptedException,
            RejectedExecutionException {
        final RuntimeException cause = new RuntimeException();
        LightFuture<Void> future = threadPool.submit(() -> {
            throw cause;
        });

        try {
            future.get();
        } catch (LightExecutionException e) {
            assertEquals(cause, e.getCause());
        }
    }

    @Test
    public void testTasksAreNotDroppedOrShuffled() throws LightExecutionException,
            InterruptedException,
            RejectedExecutionException {
        LightFuture[] futures = new LightFuture[NUM_THREADS];

        for (int i = 0; i < futures.length; i++) {
            int finalI = i;
            futures[i] = threadPool.submit(() -> finalI * finalI / 2);
        }

        for (int i = 0; i < futures.length; i++) {
            assertEquals(i * i / 2, futures[i].get());
        }
    }

    @Test
    public void testThenApply() throws LightExecutionException,
            InterruptedException,
            RejectedExecutionException {
        final int value = 1;
        final int thenApplyCount = 30;

        LightFuture<Integer> future = threadPool.submit(() -> value);
        for (int i = 0; i < thenApplyCount; i++) {
             future = future.thenApply(a -> a + a);
        }

        assertEquals(1 << thenApplyCount, future.get().intValue());
    }

    @Test(expected = LightExecutionException.class)
    public void testThenApplyException() throws LightExecutionException,
            InterruptedException,
            RejectedExecutionException {
        threadPool.submit((Supplier<Integer>) () -> {
            throw new RuntimeException();
        }).thenApply(x -> x + 1).get();
    }

    @Test
    public void testIsReady() throws LightExecutionException,
            InterruptedException,
            RejectedExecutionException {
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

    @Test
    public void testDataRacesOnSubmit() throws LightExecutionException,
            InterruptedException {
        Thread[] producers = new Thread[NUM_THREADS];
        final int numTasks = 100;
        LightFuture[] futures = new LightFuture[NUM_THREADS * numTasks];

        for (int i = 0; i < NUM_THREADS; i++) {
            final int finalI = i;
            producers[i] = new Thread(() -> {
                for (int j = 0; j < numTasks; j++) {
                    final int finalJ = j;
                    try {
                        futures[finalI * numTasks + j] =
                                threadPool.submit(() -> finalI * numTasks + finalJ);
                    } catch (RejectedExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        for (Thread producer : producers) {
            producer.start();
        }

        for (Thread producer : producers) {
            producer.join();
        }

        for (int i = 0; i < NUM_THREADS * numTasks; i++) {
            assertEquals(i, futures[i].get());
        }
    }

    @Test
    public void testGetFromDifferentThreads() throws RejectedExecutionException,
            InterruptedException {
        final int second = 1000;
        final int numberOfTasks = 50;
        final Random rng = new Random();
        final LightFuture[] futures = new LightFuture[NUM_THREADS];

        for (int i = 0; i < numberOfTasks; i++) {
            final int finalI = i;
            futures[i] = threadPool.submit(() -> {
                try {
                    Thread.sleep(second);
                } catch (InterruptedException ignored) { }

                return finalI;
            });
        }

        Thread[] waiters = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            waiters[i] = new Thread(() -> {
                for (int j = 0; j < numberOfTasks; j++) {
                    try {
                        futures[rng.nextInt(numberOfTasks)].get();
                    } catch (LightExecutionException
                            | InterruptedException ignored) { }
                }
            });
        }

        for (Thread waiter: waiters) {
            waiter.start();
        }

        for (Thread waiter: waiters) {
            waiter.join();
        }
    }

    @Test(expected = RejectedExecutionException.class)
    public void testShutdown() throws RejectedExecutionException {
        threadPool.shutdown();
        assertTrue(threadPool.isShutdown());
        threadPool.submit(() -> 0);
    }
}
