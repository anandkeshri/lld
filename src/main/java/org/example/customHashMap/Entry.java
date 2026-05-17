package org.example.customHashMap;

public class Entry {
    private final String key;
    private String value;
    private Entry next;

    public Entry(String key, String value){
        this.key = key;
        this.value = value;
        this.next = null;
    }
    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Entry getNext() {
        return next;
    }

    public void setNext(Entry next) {
        this.next = next;
    }
}
