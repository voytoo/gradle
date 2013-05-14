package org.gradle.api.internal.changedetection.state;

import com.google.common.collect.MapMaker;
import org.gradle.cache.PersistentIndexedCache;

import java.util.concurrent.ConcurrentMap;

/**
 * by Szczepan Faber on 5/14/13
 */
public class InMemoryDelegatingCache<K,V> implements PersistentIndexedCache<K, V> {
    private PersistentIndexedCache<K,V> delegate;
    private static final ConcurrentMap<Object, Answer> cache = new MapMaker().softValues().makeMap();

    public InMemoryDelegatingCache(PersistentIndexedCache<K,V> delegate) {
        this.delegate = delegate;
    }

    static class Answer {
        Object answer;

        public Answer(Object answer) {
            this.answer = answer;
        }
    }

    public V get(K key) {
        Answer out = cache.get(key);
        if (out != null) {
            return (V) out.answer;
        }
        V actual = delegate.get(key);
        cache.put(key, new Answer(actual));
        return actual;
    }

    public void put(K key, V value) {
        cache.put(key, new Answer(value));
        delegate.put(key, value);
    }

    public void remove(K key) {
        cache.remove(key);
        delegate.remove(key);
    }
}
