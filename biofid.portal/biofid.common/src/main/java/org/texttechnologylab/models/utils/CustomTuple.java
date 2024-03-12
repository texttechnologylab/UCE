package org.texttechnologylab.models.utils;

public class CustomTuple<T, K, J> {

    private T item1;
    private K item2;
    private J item3;

    public CustomTuple(T item1, K item2, J item3){
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
    }

    public void setItem3(J item3) {
        this.item3 = item3;
    }

    public J getItem3() {
        return item3;
    }

    public void setItem2(K item2) {
        this.item2 = item2;
    }

    public void setItem1(T item1) {
        this.item1 = item1;
    }

    public K getItem2() {
        return item2;
    }

    public T getItem1() {
        return item1;
    }
}
