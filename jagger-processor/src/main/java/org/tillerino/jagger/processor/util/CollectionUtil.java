package org.tillerino.jagger.processor.util;

import java.util.*;
import org.apache.commons.lang3.exception.ContextedRuntimeException;

public class CollectionUtil {
    public static <T> List<T> append(T head, Collection<T> tail) {
        List<T> l = new ArrayList<>(tail.size() + 1);
        l.add(head);
        l.addAll(tail);
        return l;
    }

    public static <T> List<T> append(Collection<T> front, Collection<T> back) {
        List<T> l = new ArrayList<>(back.size() + 1);
        l.addAll(front);
        l.addAll(back);
        return l;
    }

    public static <K, V> LinkedHashMap<K, V> mapLists(Collection<K> keys, Collection<V> values) {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        Iterator<K> k = keys.iterator();
        Iterator<V> v = values.iterator();
        while (k.hasNext() && v.hasNext()) {
            map.put(k.next(), v.next());
        }
        if (k.hasNext() || v.hasNext()) {
            throw new ContextedRuntimeException("Collections mismatched");
        }
        return map;
    }
}
