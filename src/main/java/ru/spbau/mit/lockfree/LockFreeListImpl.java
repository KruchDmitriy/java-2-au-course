package ru.spbau.mit.lockfree;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeListImpl<E> implements LockFreeList<E> {
    private final Node<E> tail = new Node<>(null, null);
    private final Node<E> head = new Node<>(tail, null);

    @Override
    public void append(E value) {
        final Node<E> node = new Node<>(tail, value);

        while (true) {
            Node<E> curr = findLast();

            if (curr.next.compareAndSet(tail, node, false, false)) {
                return;
            }
        }
    }

    @Override
    public boolean remove(E value) {
        while (true) {
            Node<E> pred = findBefore(value);
            Node<E> curr = pred.getNext();

            if (curr == tail) {
                return false;
            }

            if (!pred.next.compareAndSet(curr, curr, false, true)) {
                continue;
            }

            Node<E> succ = curr.getNext();
            pred.next.compareAndSet(curr, succ, true, false);
            return true;
        }
    }

    @Override
    public boolean contains(E value) {
        final Node<E> node = findBefore(value).getNext();
        return ! (node == tail);
    }

    @Override
    public boolean isEmpty() {
        // FIXME
        return head.next.compareAndSet(tail, tail, false, false);
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

        boolean nextIsMarked() {
            return next.isMarked();
        }
    }

    private Node<E> findBefore(E value) {
        while (true) {
            Node<E> pred = head;
            Node<E> curr = pred.getNext();
            Node<E> succ;

            while (true) {
                succ = curr.getNext();
                boolean currWasDeleted = pred.nextIsMarked();

                if (currWasDeleted) {
                    if (!pred.next.compareAndSet(curr, succ, true, false))
                        break;
                } else {
                    final E curValue = curr.value;
                    if (curr == tail || curValue.equals(value)) {
                        return pred;
                    }
                }

                pred = curr;
                curr = succ;
            }
        }
    }

    private Node<E> findLast() {
        return findBefore(null);
    }
}
