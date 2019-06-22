package com.aanchev.parser;

import java.util.AbstractList;
import java.util.function.IntFunction;


public class LazyList<E> extends AbstractList<E> {

    private E[] elements;
    private IntFunction<E> fetch;

    protected LazyList(E[] elements, IntFunction<E> fetch) {
        this.elements = elements;
        this.fetch = fetch;
    }

    @Override
    public E get(int index) {
        if (elements[index] == null) {
            elements[index] = fetch.apply(index);
        }
        return elements[index];
    }

    @Override
    public int size() {
        return elements.length;
    }

    @SuppressWarnings("unchecked")
    public static <E> LazyList<E> lazyList(int size, IntFunction<E> fetch) {
        return new LazyList(new Object[size], fetch);
    }

    public static <E> LazyList<E> empty() {
        return lazyList(0, null);
    }
}
