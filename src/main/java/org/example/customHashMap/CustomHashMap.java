package org.example.customHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomHashMap {
    private final double minLoadFactor;
    private final double maxLoadFactor;

    private Entry[] buckets;
    private int size;
    private final int minSize = 2;
//    private int bucketCount;
// adding a test comment to test git
    public CustomHashMap(double minLoadFactor, double maxLoadFactor){
        this.minLoadFactor = round(minLoadFactor);
        this.maxLoadFactor = round(maxLoadFactor);
        this.buckets = new Entry[minSize];
        this.size=0;
    }

    public void put(String key, String value){
        int hashVal = hash(key);
        synchronized (this) {
            int idx = hashVal % bucketsCount();
            if (buckets[idx] == null) {
                buckets[idx] = new Entry(key, value);
            } else {
                Entry p = buckets[idx];
                Entry prev = null;
                while (p != null) {
                    if (p.getKey().equals(key)) {
                        p.setValue(value);
                        return;
                    }
                    prev = p;
                    p = p.getNext();
                }
                prev.setNext(new Entry(key, value));
            }
            size++;
        }
        rehash();
    }

    public String get(String key){
        int hashVal = hash(key);
        synchronized (this) {
            int idx = hashVal % bucketsCount();
            Entry e = buckets[idx];
            while (e != null) {
                if (e.getKey().equals(key)) {
                    return e.getValue();
                }
                e = e.getNext();
            }
        }
        return "";
    }

    public String remove(String key){
        int hashVal = hash(key);
        String value = "";
        synchronized (this) {
            int idx = hashVal % bucketsCount();
            Entry e = buckets[idx];
            Entry prev = null;
            while (e != null) {
                Entry next = e.getNext();
                if (e.getKey().equals(key)) {
                    value = e.getValue();
                    if (prev == null) {
                        buckets[idx] = next;
                    } else {
                        prev.setNext(next);
                    }
                    e.setNext(null);
                    size--;
                    rehash();
                    return value;
                }
                prev = e;
                e = next;
            }
        }
        return value;
    }

    public List<String> getBucketKeys(int bucketIndex){
        ArrayList<String> keys =  new ArrayList<>();
        if(bucketIndex<0 || bucketIndex>= buckets.length){
            return keys;
        }

        Entry e = buckets[bucketIndex];
        while(e!=null){
            keys.add(e.getKey());
            e = e.getNext();
        }
        Collections.sort(keys);
        return keys;
    }

    public int size(){
        return this.size;
    }

    public int bucketsCount(){
        return buckets.length;
    }

    private int hash(String key){
        int n = key.length();
        int hashVal = n*n;
        for(int i = 0;i<n;i++){
            hashVal+= (key.charAt(i)-'a'+1);
        }
        return hashVal;
    }

    private void rehash(){
        double lf = round((double) size / bucketsCount());
        int bc = bucketsCount();
        if(lf<minLoadFactor){
            bc = Math.max(minSize, bc/2);
        }
        else if(lf>maxLoadFactor){
            bc = 2*bc;
        }
        if(bc== bucketsCount()){
            return;
        }
//        System.out.println("Re-Hashing...");
        Entry[] newBuckets = new Entry[bc];
        synchronized (this) {
            for (int i = 0; i < bucketsCount(); i++) {
                Entry e = buckets[i];
                Entry next;
                while (e != null) {
                    next = e.getNext();
                    int hashVal = hash(e.getKey());
                    int newIndex = hashVal % bc;
                    if (newBuckets[newIndex] == null) {
                        newBuckets[newIndex] = e;
                    } else {
                        Entry p = newBuckets[newIndex];
                        while (p.getNext() != null) {
                            p = p.getNext();
                        }
                        p.setNext(e);
                    }
                    e.setNext(null);
                    e = next;
                }
            }
            buckets = newBuckets;
        }
    }

    private double round(double num){
        return  Math.round(num*100.0)/100.0;
    }
}
