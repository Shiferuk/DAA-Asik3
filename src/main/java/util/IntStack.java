package util;

import java.util.Arrays;

public final class IntStack {

    private int[] data = new int[16];
    private int size;
    private int maxSize;
    private long pushes;

    public void push(int x) {
        if (size == data.length) {
            data = Arrays.copyOf(data, size * 2);
        }
        data[size++] = x;
        pushes++;
        if (size > maxSize) {
            maxSize = size;
        }
    }

    public int pop() {
        if (size == 0) {
            throw new IllegalStateException("stack is empty");
        }
        return data[--size];
    }

    public int peek() {
        if (size == 0) {
            throw new IllegalStateException("stack is empty");
        }
        return data[size - 1];
    }

    public boolean isEmpty() { return size == 0; }

    public int size() { return size; }

    public int maxSize() { return maxSize; }

    public long pushes() { return pushes; }
}
