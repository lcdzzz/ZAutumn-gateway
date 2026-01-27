package indi.lcdzzz.gateway.utils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Pair<K, V> {
    public final K k;
    public final V v;

    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public static <KEY, VALUE> Pair<KEY, VALUE> of(KEY key, VALUE value) {
        return new Pair<KEY, VALUE>(key, value);
    }

    public static <KEY, VALUE> Collector<Pair<? extends KEY, ? extends VALUE>, ?, Map<KEY, VALUE>> getMapCollector() {
        return Collectors.toMap(Pair::getKey, Pair::getValue);
    }

    public K getKey() {
        return this.k;
    }

    public V getValue() {
        return this.v;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Pair<?, ?> pair = (Pair)o;
            return Objects.equals(this.k, pair.k) && Objects.equals(this.v, pair.v);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.k, this.v});
    }

    public String toString() {
        return "{\"key\":\"" + this.k + "\", \"value\":\"" + this.v + "\"}";
    }
}
