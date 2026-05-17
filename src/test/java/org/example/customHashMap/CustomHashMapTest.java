package org.example.customHashMap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomHashMapTest {

    @Test
    void test1() {
        CustomHashMap map = new CustomHashMap(0.25, 0.75);
        map.put("a", "one");
        map.put("bb", "two");
//        System.out.println(map.get("a"));
        assertEquals("one", map.get("a"));
        assertEquals("", map.get("x"));
//        System.out.println(map.get("x"));

        map.put("a", "ONE");
        assertEquals("ONE", map.get("a"));
//        System.out.println(map.get("a"));
        assertEquals("two", map.remove("bb"));
//        System.out.println(map.remove("bb"));
//        System.out.println(map.remove("bb"));
        assertEquals("", map.remove("bb"));
        System.out.println(map.size() );
        assertEquals(1, map.size());
    }

    @Test
    void test2(){
        CustomHashMap map = new CustomHashMap(0.25, 0.75);  // bucketsCount initially 2

// bucketIndex = hash(key) % 2
// hash("a")  = 1*1 + 1  = 2  => 2%2 = 0
// hash("c")  = 1*1 + 3  = 4  => 4%2 = 0   (collision: same bucketIndex)

        map.put("g", "v1");
        map.put("c", "v2");
        List<String> list = map.getBucketKeys(0);
        assertEquals(2, list.size());
        assertEquals("c", list.get(0));
        assertEquals("g", list.get(1));
        list = map.getBucketKeys(1);
        assertEquals(0, list.size());
        list = map.getBucketKeys(10);
        assertEquals(0, list.size());
//        print(map.getBucketKeys(0)); //=> ["a", "c"]
//        print(map.getBucketKeys(1)); //=> []
//        print(map.getBucketKeys(10)); //=> []   // out of bounds

    }

    @Test
    void test3(){
        CustomHashMap map = new CustomHashMap(0.25, 0.75);  // bucketsCount = 2

        map.put("a", "1");    // size=1, LF=round2(1/2)=0.50  (ok)
        map.put("b", "2");    // size=2, LF=round2(2/2)=1.00  (> 0.75) => grow
// after grow: bucketsCount becomes 4 (and entries are rehashed)

        assertEquals(4, map.bucketsCount());
        assertEquals(2, map.size());
//        System.out.println(map.bucketsCount()); // => 4
//        System.out.println(map.size()) ;       // => 2
    }

    @Test
    void test4(){
        CustomHashMap map = new CustomHashMap(0.25, 0.75);
// bucketsCount = 2

        assertEquals(2, map.bucketsCount());

        map.put("a",    "1");   // size=1, LF=round2(1/2)=0.50  -> no rehash
        map.put("bb",   "2");   // size=2, LF=round2(2/2)=1.00  -> 1.00 > 0.75 => REHASH GROW to 4
        assertEquals(4, map.bucketsCount());
// After rehash to 4 buckets, entries redistribute using (hash % 4):
// "a"  : 2%4  = 2
// "bb" : 8%4  = 0
        List<String> list = map.getBucketKeys(0);
        assertEquals(1, list.size());
        assertEquals("bb", list.get(0));

        list = map.getBucketKeys(2);
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));

        map.put("abcd", "3");   // size=3, buckets=4, LF=round2(3/4)=0.75 -> not greater => no rehash
// "abcd" goes to 26%4 = 2 (collision with "a" in bucket 2)
        assertEquals(3, map.size());
        list = map.getBucketKeys(2);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals("abcd", list.get(1));

        map.put("m",    "4");   // size=4, buckets=4, LF=round2(4/4)=1.00 -> 1.00 > 0.75 => REHASH GROW to 8
        assertEquals(4, map.size());
        assertEquals(8, map.bucketsCount());
// Key movement highlight:
// before grow (4 buckets): "m" was in bucket 14%4 = 2
// after  grow (8 buckets): "m" moves to bucket 14%8 = 6
        list = map.getBucketKeys(6);
        assertEquals(1, list.size());
        assertEquals("m", list.get(0));

        map.put("zzz",  "5");   // size=5, buckets=8, LF=round2(5/8)=0.63 -> no rehash
        assertEquals(5, map.size());
        assertEquals(8, map.bucketsCount());

        list = map.getBucketKeys(0);
        assertEquals(1, list.size());
        assertEquals("bb", list.get(0));

        list = map.getBucketKeys(2);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals("abcd", list.get(1));

        list = map.getBucketKeys(6);
        assertEquals(1, list.size());
        assertEquals("m", list.get(0));

        list = map.getBucketKeys(7);
        assertEquals(1, list.size());
        assertEquals("zzz", list.get(0));

        list = map.getBucketKeys(1);
        assertEquals(0, list.size());
    }

    @Test
    void testShrink(){
        CustomHashMap map = new CustomHashMap(0.50, 0.75);
        map.put("a",    "1");   // size=1, LF=round2(1/2)=0.50  -> no rehash
        map.put("bb",   "2");
        map.put("abcd", "3");
        map.put("m",    "4");   // size=4, buckets=4, LF=round2(4/4)=1.00 -> 1.00 > 0.75 => REHASH GROW to 8
        assertEquals(4, map.size());
        assertEquals(8, map.bucketsCount());

        assertEquals("3", map.remove("abcd"));
        assertEquals(3, map.size());
        assertEquals(4, map.bucketsCount());
    }
    void print(List<String> list){
        for(String str : list){
            System.out.printf("%s, ", str);
        }
        System.out.println();
    }
}