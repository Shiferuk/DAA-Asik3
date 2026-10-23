package util;

public final class IntQueue {

    private final int[] data;
    private int head;
    private int size;
    private int maxSize;

    public IntQueue(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0");
        }
        data = new int[Math.max(1, capacity)];
    }

    public void push(int x) {
        if (size == data.length) {
            throw new IllegalStateException("queue is full (capacity " + data.length + ")");
        }
        data[(head + size) % data.length] = x;
        size++;
        if (size > maxSize) {
            maxSize = size;
        }
    }

    public int poll() {
        if (size == 0) {
            throw new IllegalStateException("queue is empty");
        }
        int x = data[head];
        head = (head + 1) % data.length;
        size--;
        return x;
    }

    public boolean isEmpty() { return size == 0; }

    public int size() { return size; }

    public int maxSize() { return maxSize; }
}
