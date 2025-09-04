package org.texttechnologylab.uce.common.utils;

public class MutableContainer<T>{
    private T value;

    public MutableContainer(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
