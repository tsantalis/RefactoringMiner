/*
 * Copyright (c) 2020 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitoinline;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;

public final class ConstructionMockTest {

    @Test
    public void testConstructionMockSimple() {
        assertEquals("foo", new Dummy().foo());
        try (MockedConstruction<Dummy> ignored = Mockito.mockConstruction(Dummy.class)) {
            assertNull(new Dummy().foo());
        }
        assertEquals("foo", new Dummy().foo());
    }

    @Test
    public void testConstructionMockCollection() {
        try (MockedConstruction<Dummy> dummy = Mockito.mockConstruction(Dummy.class)) {
            assertEquals(0, dummy.constructed().size());
            Dummy mock = new Dummy();
            assertEquals(1, dummy.constructed().size());
            assertTrue(dummy.constructed().contains(mock));
        }
    }

    @Test
    public void testConstructionMockDefaultAnswer() {
        try (MockedConstruction<Dummy> ignored = Mockito.mockConstructionWithAnswer(Dummy.class, invocation -> "bar")) {
            assertEquals("bar", new Dummy().foo());
        }
    }

    @Test
    public void testConstructionMockDefaultAnswerMultiple() {
        try (MockedConstruction<Dummy> ignored = Mockito.mockConstructionWithAnswer(Dummy.class, invocation -> "bar", invocation -> "qux")) {
            assertEquals("bar", new Dummy().foo());
            assertEquals("qux", new Dummy().foo());
            assertEquals("qux", new Dummy().foo());
        }
    }

    @Test
    public void testConstructionMockPrepared() {
        try (MockedConstruction<Dummy> ignored = Mockito.mockConstruction(Dummy.class, (mock, context) -> when(mock.foo()).thenReturn("bar"))) {
            assertEquals("bar", new Dummy().foo());
        }
    }


    @Test
    public void testConstructionMockContext() {
        try (MockedConstruction<Dummy> ignored = Mockito.mockConstruction(Dummy.class, (mock, context) -> {
            assertEquals(1, context.getCount());
            assertEquals(Collections.singletonList("foobar"), context.arguments());
            assertEquals(mock.getClass().getDeclaredConstructor(String.class), context.constructor());
            when(mock.foo()).thenReturn("bar");
        })) {
            assertEquals("bar", new Dummy("foobar").foo());
        }
    }

    @Test
    public void testConstructionMockDoesNotAffectDifferentThread() throws InterruptedException {
        try (MockedConstruction<Dummy> ignored = Mockito.mockConstruction(Dummy.class)) {
            Dummy dummy = new Dummy();
            when(dummy.foo()).thenReturn("bar");
            assertEquals("bar", dummy.foo());
            verify(dummy).foo();
            AtomicReference<String> reference = new AtomicReference<>();
            Thread thread = new Thread(() -> reference.set(new Dummy().foo()));
            thread.start();
            thread.join();
            assertEquals("foo", reference.get());
            when(dummy.foo()).thenReturn("bar");
            assertEquals("bar", dummy.foo());
            verify(dummy, times(2)).foo();
        }
    }

    @Test
    public void testConstructionMockCanCoexistWithMockInDifferentThread() throws InterruptedException {
        try (MockedConstruction<Dummy> ignored = Mockito.mockConstruction(Dummy.class)) {
            Dummy dummy = new Dummy();
            when(dummy.foo()).thenReturn("bar");
            assertEquals("bar", dummy.foo());
            verify(dummy).foo();
            AtomicReference<String> reference = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try (MockedConstruction<Dummy> ignored2 = Mockito.mockConstruction(Dummy.class)) {
                    Dummy other = new Dummy();
                    when(other.foo()).thenReturn("qux");
                    reference.set(other.foo());
                }
            });
            thread.start();
            thread.join();
            assertEquals("qux", reference.get());
            assertEquals("bar", dummy.foo());
            verify(dummy, times(2)).foo();
        }
    }

    @Test
    public void testConstructionMockMustBeExclusiveInScopeWithinThread() {
        assertThatThrownBy(
                () -> {
                    try (
                            MockedConstruction<Dummy> dummy = Mockito.mockConstruction(Dummy.class);
                            MockedConstruction<Dummy> duplicate = Mockito.mockConstruction(Dummy.class)) {
                    }
                })
                .isInstanceOf(MockitoException.class)
                .hasMessageContaining("static mocking is already registered in the current thread");
    }

    @Test
    public void testConstructionMockMustNotTargetAbstractClass() {
        assertThatThrownBy(
                () -> {
                    Mockito.mockConstruction(Runnable.class).close();
                })
                .isInstanceOf(MockitoException.class)
                .hasMessageContaining("It is not possible to construct primitive types or abstract types");
    }

    static class Dummy {


        public Dummy() {
        }

        public Dummy(String value) {
        }

        String foo() {
            return "foo";
        }
    }
}
