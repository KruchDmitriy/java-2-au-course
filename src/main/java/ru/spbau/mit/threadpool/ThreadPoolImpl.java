package ru.spbau.mit;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadPoolImpl implements ThreadPool {
    private final int numThreads;
    private final Queue<PackagedTask> tasksQueue = new ArrayDeque<>();
    private final Thread[] workers;
    private volatile boolean isShutdown;

    public ThreadPoolImpl(int numThreads) {
        this.numThreads = numThreads;

        workers = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            workers[i] = new Thread(new Worker());
            workers[i].start();
        }
    }

    @Override
    public <T> LightFuture<T> submit(Supplier<T> task) throws RejectedExecutionException {
        if (isShutdown) {
            throw new RejectedExecutionException();
        }

        synchronized (tasksQueue) {
            PackagedTask<T> packagedTask = new PackagedTask<>(task);
            tasksQueue.add(packagedTask);
            tasksQueue.notify();

            return packagedTask.getFuture();
        }
    }

    @Override
    public void shutdown() {
        isShutdown = true;

        LightFuture[] futures;
        synchronized (tasksQueue) {
            futures = new LightFuture[tasksQueue.size()];

            Iterator<PackagedTask> iterator = tasksQueue.iterator();
            for (int i = 0; iterator.hasNext(); i++) {
                futures[i] = iterator.next().getFuture();
            }
        }

        for (LightFuture future : futures) {
            try {
                future.get();
            } catch (LightExecutionException | InterruptedException ignored) { }
        }

        for (Thread worker: workers) {
            worker.interrupt();
        }
    }

    @Override
    public int getNumThreads() {
        return numThreads;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    private PackagedTask getTask() throws InterruptedException {
        synchronized (tasksQueue) {
            while (tasksQueue.isEmpty()) {
                tasksQueue.wait();
            }

            return tasksQueue.poll();
        }
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            PackagedTask task = null;
            while (!Thread.interrupted()) {
                if (task == null) {
                    try {
                        task = getTask();
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                task.execute();

                final LightFutureImpl future = task.getFuture();
                synchronized (future) {
                    task = future.getNextTask();
                }
            }
        }
    }

    private class PackagedTask<T> {
        private final Supplier<T> task;
        private final LightFutureImpl<T> lightFuture = new LightFutureImpl<>();

        PackagedTask(Supplier<T> task) {
            this.task = task;
        }

        LightFutureImpl<T> getFuture() {
            return lightFuture;
        }

        void execute() {
            try {
                T result = task.get();
                lightFuture.setResult(result);
            } catch (Exception e) {
                lightFuture.setWasThrown(e);
            }
        }
    }

    private class LightFutureImpl<T> implements LightFuture<T> {
        private boolean isReady;
        private T result;
        private Throwable wasThrown;
        private PackagedTask nextTask;

        @Override
        public synchronized T get() throws LightExecutionException, InterruptedException {
            while (!isReady) {
                wait();
            }

            if (wasThrown != null) {
                throw new LightExecutionException(wasThrown);
            }

            return result;
        }

        @Override
        public synchronized <R> LightFuture<R> thenApply(Function<T, R> function)
                throws RejectedExecutionException {
            Supplier<R> supplier = () -> {
                try {
                    return function.apply(get());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            if (isReady) {
                return submit(supplier);
            }

            nextTask = new PackagedTask<>(supplier);

            return nextTask.getFuture();
        }

        @Override
        public boolean isReady() {
            return isReady;
        }

        private synchronized void setResult(T result) {
            this.result = result;
            isReady = true;
            notifyAll();
        }

        private synchronized void setWasThrown(Throwable wasThrown) {
            this.wasThrown = wasThrown;
            isReady = true;
            notifyAll();
        }

        PackagedTask getNextTask() {
            return nextTask;
        }
    }
}
