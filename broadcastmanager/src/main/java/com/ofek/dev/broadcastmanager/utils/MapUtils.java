package com.ofek.dev.broadcastmanager.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapUtils {


    public interface Producer<T> {
        T produce();
    }
    public interface Procedure<T> {
        void run(T t);
    }
    public static final <K,V> V getOrPut(Map<K,V> m, K k, Producer<V> p) {
        if (m.containsKey(k))
            return m.get(k);
        else {
            V produce = p.produce();
            m.put(k, produce);
            return produce;
        }

    }
    public static final <K,V> V getOrDefault(Map<K,V> m, K k,Producer<V> p) {
        if (m.containsKey(k))
            return m.get(k);
        else {
            return p.produce();
        }

    }
    public static final <K,V> void ifExists(Map<K,V> m, K k,Procedure<V> p) {
        if (m.containsKey(k))
            p.run(m.get(k));
    }
    public static <K,V>  void removeIfInList(Map<K,? extends List<V>> m, K k, V v) {
        ifExists(m, k, (list) -> list.remove(v));
    }
    public static <K,V> void removeIfInSet(Map<K,? extends Set<V>> m, K k, V v) {
        ifExists(m, k, (set) -> set.remove(v));
    }
    public static final <K,V> void ifNotExists(Map<K,V> m, K k,Runnable r) {
        if (!m.containsKey(k))
            r.run();
    }
    public static final <K,V> List<V> putInList(Map<K, List<V>> m, K k, V v, Producer<List<V>> p) {
        List<V> list = getOrPut(m, k, p);
        list.add(v);
        return list;
    }
    public static final <K,V> Set<V> putInSet(Map<K, Set<V>> m, K k, V v, Producer<Set<V>> p) {
        Set<V> set = getOrPut(m, k, p);
        set.add(v);
        return set;
    }
}
