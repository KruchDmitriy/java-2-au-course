package ru.spbau.mit;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadPoolImpl implements ThreadPool {
    private final int numThreads;
    private final Queue<PackagedTask> tasksQueue = new ArrayDeque<>();
    private Worker[] workers;

    public ThreadPoolImpl(int numThreads) {
        this.numThreads = numThreads;

        workers = new Worker[numThreads];
        for (int i = 0; i < numThreads; i++) {
            workers[i] = new Worker();
            workers[i].start();
        }
    }

    @Override
    public <T> LightFuture<T> submit(Supplier<T> task) {
        synchronized (tasksQueue) {
            PackagedTask<T> packagedTask = new PackagedTask<>(task);
            tasksQueue.add(packagedTask);
            tasksQueue.notify();

            return packagedTask.getFuture();
        }
    }

    @Override
    public void shutdown() {
        for (Worker worker: workers) {
            worker.interrupt();
        }
    }

    public int getNumThreads() {
        return numThreads;
    }

    private PackagedTask getTask() throws InterruptedException {
        synchronized (tasksQueue) {
            while (tasksQueue.size() == 0) {
                tasksQueue.wait();
            }

            return tasksQueue.poll();
        }
    }

    private class Worker extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                PackagedTask task;

                try {
                    task = getTask();
                } catch (InterruptedException e) {
                    return;
                }

                task.execute();
            }
        }
    }

    private class PackagedTask<T> {
        private final Supplier<T> task;
        private LightFutureImpl<T> lightFuture = new LightFutureImpl<>();

        PackagedTask(Supplier<T> task) {
            this.task = task;
        }

        LightFuture<T> getFuture() {
            return lightFuture;
        }

        void execute() {
            try {
                T result = task.get();
                lightFuture.setResult(result);
            } catch (Exception e) {
                lightFuture.setWasThrown();
            }
        }
    }

    private class LightFutureImpl<T> implements LightFuture<T> {
        private Boolean isReady = false;
        private T result;
        private boolean wasThrown = false;

        @Override
        public synchronized T get() throws LightExecutionException, InterruptedException {
            while (!isReady) {
                wait();
            }

            if (wasThrown) {
                throw new LightExecutionException();
            }

            return result;
        }

        @Override
        public <R> LightFuture<R> thenApply(Function<T, R> function) {
            return submit(() -> {
                try {
                    return function.apply(get());
                } catch (LightExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public boolean isReady() {
            return isReady;
        }

        private synchronized void setResult(T result) {
            this.result = result;
            isReady = true;
            notify();
        }

        private synchronized void setWasThrown() {
            wasThrown = true;
            isReady = true;
            notify();
        }
    }
}
