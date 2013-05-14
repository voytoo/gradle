/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.cache.PersistentCache;
import org.gradle.cache.PersistentIndexedCache;
import org.gradle.cache.internal.ReferencablePersistentCache;
import org.gradle.internal.Factory;
import org.gradle.listener.LazyCreationProxy;
import org.gradle.messaging.serialize.Serializer;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.gradle.util.Clock.prettyTime;

/**
 * by Szczepan Faber, created at: 2/27/13
 */
public class LibrarianThread<K, V> {

    private final static Logger LOG = Logging.getLogger(LibrarianThread.class);
    private Librarian librarian;
    private Thread thread;
    private PersistentCache cache;
    private boolean parallel;
    private final Factory<PersistentCache> cacheFactory;

    public LibrarianThread(boolean parallel, Factory<PersistentCache> cacheFactory) {
        this.parallel = parallel;
        this.cacheFactory = cacheFactory;
        //creation of the cache must happen in the thread so that lock is owned by correct thread
        this.librarian = new Librarian();
        thread = new Thread(librarian);
    }

    public void stop() {
        librarian.requestStop();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void start() {
        if (parallel) {
            thread.start();
        } else {
            cache = cacheFactory.create();
        }
    }

    public boolean isStopped() {
        return librarian.stopped;
    }

    public PersistentIndexedCache<K, V> sync(final PersistentIndexedCache delegate) {
        if (parallel) {
            return new PersistentIndexedCache<K, V>() {
                public V get(K key) {
                    return librarian.get(key, delegate);
                }

                public void put(K key, V value) {
                    librarian.put(key, value, delegate);
                }

                public void remove(K key) {
                    librarian.remove(key, delegate);
                }
            };
        } else {
            return delegate;
        }
    }

    public PersistentCache getCache() {
        if (cache == null) {
            throw new IllegalStateException("Librarian thread has not been started yet");
        }
        return cache;
    }

    public PersistentIndexedCache<K, V> createCache(final String cacheName, final Class<K> keyType, final Class<V> valueType) {
        Factory<PersistentIndexedCache> factory = new Factory<PersistentIndexedCache>() {
            public PersistentIndexedCache create() {
                return getCache().createCache(cacheFile(cacheName), keyType, valueType);
            }
        };
        return createCache(factory);
    }

    private PersistentIndexedCache<K, V> createCache(Factory<PersistentIndexedCache> factory) {
        PersistentIndexedCache lazy = new LazyCreationProxy<PersistentIndexedCache>(PersistentIndexedCache.class, factory).getSource();
        return sync(lazy);
    }

    private File cacheFile(String cacheName) {
        return new File(getCache().getBaseDir(), cacheName + ".bin");
    }

    public PersistentIndexedCache<K, V> createCache(final String cacheName, final Class<K> keyType, final Serializer<V> valueSerializer) {
        Factory<PersistentIndexedCache> factory = new Factory<PersistentIndexedCache>() {
            public PersistentIndexedCache create() {
                return getCache().createCache(cacheFile(cacheName), keyType, valueSerializer);
            }
        };
        return createCache(factory);
    }

    public void addCacheUser() {
        librarian.addUser();
    }

    public void removeCacheUser() {
        librarian.removeUser();
    }

    public void useCache(Runnable runnable) {
        if (parallel) {
            librarian.addUser();
            try {
                runnable.run();
            } finally {
                librarian.removeUser();
            }
        } else {
            cache.useCache("locking", runnable);
        }
    }

    public void longRunningOperation(Runnable runnable) {
        if (parallel) {
            librarian.removeUser();
            try {
                runnable.run();
            } finally {
                librarian.addUser();
            }
        } else {
            cache.longRunningOperation("unlocking", runnable);
        }
    }

    enum State { reading, writing, idle }

    private class Librarian implements Runnable {

        private final Lock lock = new ReentrantLock();
        private final Condition accessRequested = lock.newCondition();
        private final Condition answerReady = lock.newCondition();
        private final LinkedList<Factory<V>> readQueue = new LinkedList<Factory<V>>();
        private final Map<Object, Answer<V>> answers = new HashMap<Object, Answer<V>>();
        private final LinkedList<Runnable> writes = new LinkedList<Runnable>();
        private boolean stopRequested;
        private boolean stopped;
        private long blockedByReading;
        private long blockedByReadingCount;
        private long blockedByWriting;
        private long blockedByWritingCount;
        private long blockedByIdle;
        private long blockedByIdleCount;
        private long readRequests;
        private long writeRequests;
        private long totalIdle;
        private int unlockedCache;
        private int cacheUnlockingFrequency = 0;//Integer.valueOf(System.getProperty("cacheFrequency", "2000"));
        private long totalCacheUnlocked;
        private int users;
        private State state;

        public V get(final K key, final PersistentIndexedCache delegate) {
            lock.lock();
            try {
                return waitFor(new Factory<V>() {
                    public V create() {
                        V out = (V) delegate.get(key);
                        return out;
                    }
                });
            } finally {
                lock.unlock();
            }
        }

        public void put(final K key, final V value, final PersistentIndexedCache delegate) {
            lock.lock();
            try {
                writes.add(new Runnable() {
                    public void run() {
                        delegate.put(key, value);
                    }
                });
                accessRequested.signalAll();
            } finally {
                lock.unlock();
            }
        }

        //when read for given key, check if there is a write/remove pending, if there is, use the value from write
        // instead of getting it from the delegate cache
        //when there are multiple writes for the same key (e.g. check when the write/remove is queued), ignore the earlier one

        public void remove(final K key, final PersistentIndexedCache delegate) {
            lock.lock();
            try {
                writes.add(new Runnable() {
                    public void run() {
                        delegate.remove(key);
                    }
                });
                accessRequested.signalAll();
            } finally {
                lock.unlock();
            }
        }

        private V waitFor(Factory<V> factory) {
            readQueue.add(factory);
            long start = System.currentTimeMillis();
            State current = state;
            accessRequested.signalAll();
            Answer<V> answer;
            while((answer = answers.get(factory)) == null) {
                try {
                    answerReady.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (stopRequested) {
                    throw new RuntimeException("Stop requested while waiting for cache value");
                }
            }
            long duration = System.currentTimeMillis() - start;
            switch(current) {
                case reading: blockedByReading += duration; blockedByReadingCount++; break;
                case writing: blockedByWriting += duration; blockedByWritingCount++; break;
                case idle: blockedByIdle += duration; blockedByIdleCount++; break;
            }
            return answer.value;
        }

        public void run() {
            long start = System.currentTimeMillis();
            try {
                cache = cacheFactory.create();
                assert cache instanceof ReferencablePersistentCache : "Cache must be of type ReferencablePersistentCache so that we can close it.";
                cache.useCache("Librarian owns the task history cache", new Runnable() {
                    public void run() {
                        runNow();
                    }
                });
                ((ReferencablePersistentCache) cache).close();
            } catch (Throwable e) {
                LOG.error("Problems running the librarian thread", e);
            }
            long totalTime = System.currentTimeMillis() - start;
            long busyTime = totalTime - totalIdle;
            LOG.lifecycle("Task history access thread. Busy: {} (reads: {}, writes: {}), idle: {}, blocked read due to: [reading {}:{}, writing {}:{}, other {}:{}], Cache unlocked: {}, Cache unlock count: {}",
                    prettyTime(busyTime), readRequests, writeRequests, prettyTime(totalIdle), blockedByReadingCount, prettyTime(blockedByReading), blockedByWritingCount, prettyTime(blockedByWriting), blockedByIdleCount, prettyTime(blockedByIdle), prettyTime(totalCacheUnlocked), unlockedCache);
        }

        private void runNow() {
            lock.lock();
            try {
                long nextUnlock = System.currentTimeMillis() + cacheUnlockingFrequency;
                while(true) {
                    if (!readQueue.isEmpty()) {
                        state = State.reading;
                        readRequests++;
                        Factory<V> factory = readQueue.removeFirst();
                        V value = factory.create();
                        answers.put(factory, new Answer<V>(value));
                        answerReady.signalAll();
                    } else if (!writes.isEmpty()) {
                        state = State.writing;
                        writeRequests++;
                        Runnable runnable = writes.removeFirst();
                        runnable.run();
                    } else {
                        state = State.idle;
                        if (stopRequested) {
                            break;
                        }
                        long start = System.currentTimeMillis();
                        if (users == 0) {
                            nextUnlock = start + cacheUnlockingFrequency;
                            unlockedCache++;
                            cache.longRunningOperation("Librarian is idle and awaits task history cache requests", new Runnable() {
                                public void run() {
                                    await(true);
                                }
                            });
                        } else {
                            await(false);
                        }
                    }
                }
            } finally {
                lock.unlock();
            }
            stopped = true;
        }

        private void await(boolean cacheUnlocked) {
            try {
                long startAwait = System.currentTimeMillis();
                accessRequested.await();
                long duration = System.currentTimeMillis() - startAwait;
                totalIdle += duration;
                if (cacheUnlocked) {
                    totalCacheUnlocked += duration;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void requestStop() {
            lock.lock();
            stopRequested = true;
            try {
                accessRequested.signalAll();
                answerReady.signalAll();
            } finally {
                lock.unlock();
            }
        }

        public void addUser() {
            lock.lock();
            try {
                users++;
            } finally {
                lock.unlock();
            }
        }

        public void removeUser() {
            lock.lock();
            try {
                users--;
            } finally {
                lock.unlock();
            }
        }

        private class Answer<V> {
            V value;

            public Answer(V value) {
                this.value = value;
            }
        }
    }
}
