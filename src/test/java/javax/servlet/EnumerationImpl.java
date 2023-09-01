package javax.servlet;

import java.util.Enumeration;

public class EnumerationImpl<T> implements Enumeration<T> {
    private final T[] elements;

    private int index = 0;

    public EnumerationImpl(T[] elements) {
        this.elements = elements;
    }

    @Override
    public boolean hasMoreElements() {
        return index >= elements.length;
    }

    @Override
    public T nextElement() {
        return elements[index++];
    }
}
