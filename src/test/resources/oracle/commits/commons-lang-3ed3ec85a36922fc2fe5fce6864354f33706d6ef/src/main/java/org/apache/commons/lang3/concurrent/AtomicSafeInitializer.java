/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.lang3.concurrent;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A specialized {@link ConcurrentInitializer} implementation which is similar
 * to {@link AtomicInitializer}, but ensures that the {@link #initialize()}
 * method is called only once.
 *
 * <p>
 * As {@link AtomicInitializer} this class is based on atomic variables, so it
 * can create an object under concurrent access without synchronization.
 * However, it implements an additional check to guarantee that the
 * {@link #initialize()} method which actually creates the object cannot be
 * called multiple times.
 * </p>
 * <p>
 * Because of this additional check this implementation is slightly less
 * efficient than {@link AtomicInitializer}, but if the object creation in the
 * {@code initialize()} method is expensive or if multiple invocations of
 * {@code initialize()} are problematic, it is the better alternative.
 * </p>
 * <p>
 * From its semantics this class has the same properties as
 * {@link LazyInitializer}. It is a &quot;save&quot; implementation of the lazy
 * initializer pattern. Comparing both classes in terms of efficiency is
 * difficult because which one is faster depends on multiple factors. Because
 * {@link AtomicSafeInitializer} does not use synchronization at all it probably
 * outruns {@link LazyInitializer}, at least under low or moderate concurrent
 * access. Developers should run their own benchmarks on the expected target
 * platform to decide which implementation is suitable for their specific use
 * case.
 * </p>
 *
 * @since 3.0
 * @param <T> the type of the object managed by this initializer class
 */
public abstract class AtomicSafeInitializer<T> extends AbstractConcurrentInitializer<T, RuntimeException> {

    private static final Object NO_INIT = new Object();

    /** A guard which ensures that initialize() is called only once. */
    private final AtomicReference<AtomicSafeInitializer<T>> factory = new AtomicReference<>();

    /** Holds the reference to the managed object. */
    private final AtomicReference<T> reference = new AtomicReference<>((T) NO_INIT);

    /**
     * Gets (and initialize, if not initialized yet) the required object
     *
     * @return lazily initialized object
     * @throws ConcurrentException if the initialization of the object causes an
     * exception
     */
    @Override
    public final T get() throws ConcurrentException {
        T result;

        while ((result = reference.get()) == (T) NO_INIT) {
            if (factory.compareAndSet(null, this)) {
                reference.set(initialize());
            }
        }

        return result;
    }

    /**
     * Tests whether this instance is initialized. Once initialized, always returns true.
     *
     * @return whether this instance is initialized. Once initialized, always returns true.
     * @since 3.14.0
     */
    @Override
    public boolean isInitialized() {
        return reference.get() != NO_INIT;
    }
}
