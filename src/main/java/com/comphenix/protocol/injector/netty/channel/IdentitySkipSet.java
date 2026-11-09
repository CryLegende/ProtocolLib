package com.comphenix.protocol.injector.netty.channel;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A concurrent set which compares its elements by identity (==) instead of equals/hashCode.
 * <p>
 * It backs {@link NettyChannelInjector}'s skipped packet set. Packets are added on the thread sending the packet and
 * removed on the netty event loop, so a ConcurrentHashMap.newKeySet() would call hashCode() and equals() of the packet
 * on both threads. For packets that are records (such as chat packets) the hash covers the whole payload, which can be
 * tens of kilobytes of text components.
 * <p>
 * This set is deliberately lock-free: isEmpty() is called for every outbound packet from several threads, so guarding it
 * with a lock (for example Collections.synchronizedSet) would serialize all outbound traffic.
 */
final class IdentitySkipSet extends AbstractSet<Object> {

    private static final class Key {

        final Object value;
        final int hash;

        Key(Object value) {
            this.value = value;
            this.hash = System.identityHashCode(value);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key && ((Key) other).value == this.value;
        }
    }

    private final ConcurrentHashMap<Key, Boolean> map = new ConcurrentHashMap<>();

    @Override
    public boolean add(Object o) {
        return this.map.put(new Key(o), Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return this.map.remove(new Key(o)) != null;
    }

    @Override
    public boolean contains(Object o) {
        return this.map.containsKey(new Key(o));
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Iterator<Object> iterator() {
        Iterator<Key> keys = this.map.keySet().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return keys.hasNext();
            }

            @Override
            public Object next() {
                return keys.next().value;
            }

            @Override
            public void remove() {
                keys.remove();
            }
        };
    }
}
