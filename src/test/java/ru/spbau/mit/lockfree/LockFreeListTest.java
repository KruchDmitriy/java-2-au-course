package ru.spbau.mit.lockfree;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.junit.Assert.assertEquals;
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
    public void testInsertionMultiThread() throws InterruptedException {
        final int numThreads = 12;
        final int numInsertions = 100;

        final Thread[] threads = new Thread[numThreads];
        CyclicBarrier barrierAppend = new CyclicBarrier(numThreads);
        CyclicBarrier barrierContains = new CyclicBarrier(numThreads);
        CyclicBarrier barrierRemove = new CyclicBarrier(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int finalI = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < numInsertions; j++) {
                    list.append(finalI * numInsertions + j);
                }

                try {
                    barrierAppend.await();
                } catch (InterruptedException | BrokenBarrierException ignored) { }

                for (int j = 0; j < numThreads * numInsertions; j++) {
                    assertTrue(list.contains(j));
                }

                try {
                    barrierContains.await();
                } catch (InterruptedException | BrokenBarrierException ignored) { }

                for (int j = 0; j < numInsertions; j++) {
                    assertTrue(list.remove(finalI * numInsertions + j));
                }

                try {
                    barrierRemove.await();
                } catch (InterruptedException | BrokenBarrierException ignored) { }

                for (int j = 0; j < numThreads * numInsertions; j++) {
                    assertFalse(list.contains(j));
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

//        assertTrue(list.isEmpty());

        for (int j = 0; j < numThreads * numInsertions; j++) {
            if (list.remove(j)) {
                System.out.println(Math.sin(10.));
            }
        }
    }
}
