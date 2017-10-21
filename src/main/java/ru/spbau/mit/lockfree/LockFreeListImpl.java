package ru.spbau.mit.lockfree;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeListImpl<E> implements LockFreeList<E> {
    private final Node<E> tail = new Node<>(null, null);
    private final Node<E> head = new Node<>(tail, null);
    private final AtomicInteger size = new AtomicInteger();

    @Override
    public void append(E value) {
        while (true) {
            final Bounds<E> bounds = find(value);
            final Node<E> pred = bounds.pred;
            final Node<E> curr = bounds.curr;

            final Node<E> node = new Node<>(curr, value);
            if (pred.next.compareAndSet(curr, node, false, false)) {
                size.incrementAndGet();
                return;
            }
        }
    }

    @Override
    public boolean remove(E value) {
        while (true) {
            final Bounds<E> bounds = find(value);
            final Node<E> pred = bounds.pred;
            final Node<E> curr = bounds.curr;
            final Node<E> succ = curr.getNext();

            if (curr == tail || !curr.value.equals(value)) {
                return false;
            }

            if (!curr.next.attemptMark(succ, true)) {
                continue;
            }

            size.decrementAndGet();

            pred.next.compareAndSet(curr, succ, false, false);
            return true;
        }
    }

    @Override
    public boolean contains(E value) {
        final Node<E> node = find(value).curr;
        return ! (node == tail) && node.value.equals(value);
    }

    @Override
    public boolean isEmpty() {
        return size.get() == 0;
    }

    private static class Node<E> {
        AtomicMarkableReference<Node<E>> next;
        final E value;

        Node(Node<E> next, E value) {
            this.next = new AtomicMarkableReference<>(next, false);
            this.value = value;
        }

        Node<E> getNext() {
            return next.getReference();
        }

        Node<E> getNext(boolean[] mark) {
            return next.get(mark);
        }

        boolean nextIsChanged(Node<E> next) {
            return this.next.isMarked()
                    || this.next.getReference() != next;
        }

        int compare(E value) {
            return Integer.compare(this.value.hashCode() - value.hashCode(), 0);
        }
    }

    private static class Bounds<E> {
        final Node<E> pred;
        final Node<E> curr;

        Bounds(Node<E> pred, Node<E> curr) {
            this.pred = pred;
            this.curr = curr;
        }
    }

    private Bounds<E> find(E value) {
        boolean[] currWasDeleted = {false};

        Node<E> pred = head;
        Node<E> curr = pred.getNext();
        Node<E> succ;

        while (curr != tail && curr.compare(value) >= 0) {
            succ = curr.getNext(currWasDeleted);

            if (pred.nextIsChanged(curr)
                || currWasDeleted[0]
                && !pred.next.compareAndSet(curr, succ, false, false))
            {
                pred = head;
                curr = pred.getNext();
                continue;
            }

            if (!currWasDeleted[0]) {
                if (value.equals(curr.value))
                    return new Bounds<>(pred, curr);
                else
                    pred = curr;
            }

            curr = succ;
        }

        return new Bounds<>(pred, curr);
    }
}
