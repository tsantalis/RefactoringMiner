package org.codetracker.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Memoizes values derived from RefactoringMiner model objects (UMLOperation, UMLClass,
 * CompositeStatementObject, ...), keyed by object identity.
 * <p>
 * Identity is required because the UML classes override equals/hashCode with
 * signature-based semantics, so two different versions of a method would collide.
 * Keys are weakly referenced, so cached entries disappear together with the
 * UML models that own them.
 * <p>
 * The cached values must depend only on the key object and never on a Version.
 */
public final class IdentityMemo {
    private static final ConcurrentHashMap<IdentityWeakKey, Object> CACHE = new ConcurrentHashMap<>();
    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();

    private IdentityMemo() {
    }

    @SuppressWarnings("unchecked")
    public static <K, V> V get(K key, Function<K, V> compute) {
        expungeStaleEntries();
        IdentityWeakKey lookup = new IdentityWeakKey(key, null);
        Object value = CACHE.get(lookup);
        if (value == null) {
            value = compute.apply(key);
            if (value != null) {
                Object previous = CACHE.putIfAbsent(new IdentityWeakKey(key, QUEUE), value);
                if (previous != null) {
                    value = previous;
                }
            }
        }
        return (V) value;
    }

    public static void clear() {
        CACHE.clear();
    }

    private static void expungeStaleEntries() {
        Reference<?> ref;
        while ((ref = QUEUE.poll()) != null) {
            CACHE.remove(ref);
        }
    }

    private static final class IdentityWeakKey extends WeakReference<Object> {
        private final int hash;

        IdentityWeakKey(Object referent, ReferenceQueue<Object> queue) {
            super(referent, queue);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof IdentityWeakKey)) {
                return false;
            }
            Object referent = get();
            return referent != null && referent == ((IdentityWeakKey) o).get();
        }
    }
}
