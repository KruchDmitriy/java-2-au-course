package ru.spbau.mit.lockfree;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LockFreeListTest {
    private LockFreeList<Integer> list;

    @Before
    public void setUp() {
        list = new LockFreeListImpl<>();
    }

    @Test
    public void testSimple() {
        Integer value = 0;

        assertTrue(list.isEmpty());
        assertFalse(list.contains(value));

        list.append(value);

        assertTrue(list.contains(value));
        assertFalse(list.isEmpty());

        assertTrue(list.remove(value));
        assertFalse(list.remove(value));

        assertFalse(list.contains(value));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testInsertDeleteOneThread() {
        final int numInsertions = 1000;

        assertTrue(list.isEmpty());

        for (int i = 0; i < numInsertions; i++) {
            list.append(i);
        }

        assertFalse(list.isEmpty());

        for (int i = 0; i < numInsertions; i++) {
            assertTrue(list.contains(i));
        }

        assertFalse(list.isEmpty());

        for (int i = 0; i < numInsertions; i++) {
            assertTrue(list.remove(i));
        }

        assertTrue(list.isEmpty());
    }

    @Test
    public void testInsertDeleteMultiThread() throws InterruptedException {
        final int numThreads = 8;
        final int numInsertions = 1000;

        final Thread[] threads = new Thread[numThreads];
        final CyclicBarrier barrierStart = new CyclicBarrier(numThreads);
        final CyclicBarrier barrierAppend = new CyclicBarrier(numThreads);
        final CyclicBarrier barrierContains = new CyclicBarrier(numThreads);
        final CyclicBarrier barrierRemove = new CyclicBarrier(numThreads);

        for (int i = 0; i < threads.length; i++) {
            final int finalI = i;
            threads[i] = new Thread(() -> {
                try {
                    barrierStart.await();

                    for (int j = 0; j < numInsertions; j++) {
                        list.append(finalI * numInsertions + j);
                    }

                    barrierAppend.await();

                    for (int j = 0; j < numThreads * numInsertions; j++) {
                        assertTrue(list.contains(j));
                    }

                    barrierContains.await();

                    for (int j = 0; j < numInsertions; j++) {
                        assertTrue(list.remove(finalI * numInsertions + j));
                    }

                    barrierRemove.await();

                    for (int j = 0; j < numThreads * numInsertions; j++) {
                        assertFalse(list.remove(j));
                    }
                } catch (InterruptedException | BrokenBarrierException ignored) { }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertTrue(list.isEmpty());
    }

    @Test
    public void testConcurrentInsertAndDelete() throws InterruptedException {
        ConcurrentMap<Integer, Integer> map = new ConcurrentSkipListMap<>();

        final int numThreads = 8;
        final int numOperations = 10_000;
        final Random rng = new Random();
        final int randomBound = 100;

        final Thread[] threads = new Thread[numThreads];
        final ReentrantLock[] locks = new ReentrantLock[randomBound];

        for (int i = 0; i < randomBound; i++) {
            map.put(i, 0);
            locks[i] = new ReentrantLock();
        }


        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < numOperations; j++) {
                    boolean toInsert = rng.nextBoolean();
                    final Integer key = rng.nextInt(randomBound);

                    try {
                        locks[key].lock();

                        final Integer value = map.get(key);
                        if (toInsert) {
                            if (value.equals(0)) {
                                assertFalse(list.contains(key));
                            } else {
                                assertTrue(list.contains(key));
                            }

                            map.put(key, value + 1);
                            list.append(key);
                        } else {
                            if (value.equals(0)) {
                                assertFalse(list.contains(key));
                            } else {
                                assertTrue(list.contains(key));

                                map.put(key, value - 1);
                                assertTrue(list.remove(key));
                            }
                        }
                    } finally {
                        locks[key].unlock();
                    }
                }
            });
        }

        for (Thread thread: threads) {
            thread.start();
        }

        for (Thread thread: threads) {
            thread.join();
        }
    }
}
