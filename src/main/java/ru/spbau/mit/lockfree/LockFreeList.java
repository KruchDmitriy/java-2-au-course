package ru.spbau.mit.lockfree;

public interface LockFreeList<E> {
    void append(E value);
    boolean remove(E value);
    boolean contains(E value);
    boolean isEmpty();
}
