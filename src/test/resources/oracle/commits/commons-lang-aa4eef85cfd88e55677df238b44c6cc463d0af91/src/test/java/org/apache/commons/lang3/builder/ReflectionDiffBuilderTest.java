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
package org.apache.commons.lang3.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.commons.lang3.AbstractLangTest;
import org.junit.jupiter.api.Test;

public class ReflectionDiffBuilderTest extends AbstractLangTest {

    @SuppressWarnings("unused")
    private static final class TypeTestChildClass extends TypeTestClass {
        String field = "a";
    }

    @SuppressWarnings("unused")
    private static class TypeTestClass implements Diffable<TypeTestClass> {
        private static int staticField;
        private final ToStringStyle style = SHORT_STYLE;
        private final boolean booleanField = true;
        private final boolean[] booleanArrayField = {true};
        private final byte byteField = (byte) 0xFF;
        private final byte[] byteArrayField = {(byte) 0xFF};
        private char charField = 'a';
        private char[] charArrayField = {'a'};
        private final double doubleField = 1.0;
        private final double[] doubleArrayField = {1.0};
        private final float floatField = 1.0f;
        private final float[] floatArrayField = {1.0f};
        int intField = 1;
        private final int[] intArrayField = {1};
        private final long longField = 1L;
        private final long[] longArrayField = {1L};
        private final short shortField = 1;
        private final short[] shortArrayField = {1};
        private final Object objectField = null;
        private final Object[] objectArrayField = {null};
        private transient String transientField;
        @DiffExclude
        private String annotatedField = "a";
        private String excludedField = "a";

        @Override
        public DiffResult diff(final TypeTestClass obj) {
            return new ReflectionDiffBuilder(this, obj, style).setExcludeFieldNames("excludedField").build();
        }

        @Override
        public boolean equals(final Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj, false);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this, false);
        }
    }

    private static final ToStringStyle SHORT_STYLE = ToStringStyle.SHORT_PREFIX_STYLE;

    @Test
    public void test_array_difference() {
        final TypeTestClass firstObject = new TypeTestClass();
        firstObject.charArrayField = new char[] { 'c' };
        final TypeTestClass secondObject = new TypeTestClass();

        final DiffResult list = firstObject.diff(secondObject);
        assertEquals(1, list.getNumberOfDiffs());
    }

    @Test
    public void test_difference_in_inherited_field() {
        final TypeTestChildClass firstObject = new TypeTestChildClass();
        firstObject.intField = 99;
        final TypeTestChildClass secondObject = new TypeTestChildClass();

        final DiffResult list = firstObject.diff(secondObject);
        assertEquals(1, list.getNumberOfDiffs());
    }

    @Test
    public void test_no_differences() {
        final TypeTestClass firstObject = new TypeTestClass();
        final TypeTestClass secondObject = new TypeTestClass();

        final DiffResult list = firstObject.diff(secondObject);
        assertEquals(0, list.getNumberOfDiffs());
    }

    @Test
    public void test_no_differences_diff_exclude_annotated_field() {
        final TypeTestClass firstObject = new TypeTestClass();
        firstObject.annotatedField = "b";
        final TypeTestClass secondObject = new TypeTestClass();

        final DiffResult list = firstObject.diff(secondObject);
        assertEquals(0, list.getNumberOfDiffs());
    }

    @Test
    public void test_no_differences_diff_excluded_field_and_exclude_annotated_field() {
        final TypeTestClass firstObject = new TypeTestClass();
        firstObject.excludedField = "b";
        firstObject.annotatedField = "b";
        final TypeTestClass secondObject = new TypeTestClass();

        final DiffResult list = firstObject.diff(secondObject);
        assertEquals(0, list.getNumberOfDiffs());
    }

    @Test
    public void test_no_differences_excluded_field() {
        final TypeTestClass firstObject = new TypeTestClass();
        firstObject.excludedField = "b";
        final TypeTestClass secondObject = new TypeTestClass();

        final DiffResult list = firstObject.diff(secondObject);
        assertEquals(0, list.getNumberOfDiffs());
    }

    @Test
    public void test_no_differences_inheritance() {
        final TypeTestChildClass firstObject = new TypeTestChildClass();
        final TypeTestChildClass secondObject = new TypeTestChildClass();

        final DiffResult list = firstObject.diff(secondObject);
        assertEquals(0, list.getNumberOfDiffs());
    }

    @Test
    public void test_primitive_difference() {
        final TypeTestClass firstObject = new TypeTestClass();
        firstObject.charField = 'c';
        final TypeTestClass secondObject = new TypeTestClass();

        final DiffResult list = firstObject.diff(secondObject);
        assertEquals(1, list.getNumberOfDiffs());
    }

    @Test
    public void test_transient_field_difference() {
        final TypeTestClass firstObject = new TypeTestClass();
        firstObject.transientField = "a";
        final TypeTestClass secondObject = new TypeTestClass();
        firstObject.transientField = "b";

        final DiffResult list = firstObject.diff(secondObject);
        assertEquals(0, list.getNumberOfDiffs());
    }

    @Test
    public void testGetExcludeFieldNamesWithNullExcludedFieldNames() {
        final ReflectionDiffBuilder<TypeTestClass> reflectionDiffBuilder = new ReflectionDiffBuilder<>(new TypeTestClass(), new TypeTestChildClass(), SHORT_STYLE);
        reflectionDiffBuilder.setExcludeFieldNames(null);
        final String[] excludeFieldNames = reflectionDiffBuilder.getExcludeFieldNames();
        assertNotNull(excludeFieldNames);
        assertEquals(0, excludeFieldNames.length);
    }

    @Test
    public void testGetExcludeFieldNamesWithNullValuesInExcludedFieldNames() {
        final ReflectionDiffBuilder<TypeTestClass> reflectionDiffBuilder = new ReflectionDiffBuilder<>(new TypeTestClass(), new TypeTestChildClass(), SHORT_STYLE);
        reflectionDiffBuilder.setExcludeFieldNames("charField", null);
        final String[] excludeFieldNames = reflectionDiffBuilder.getExcludeFieldNames();
        assertNotNull(excludeFieldNames);
        assertEquals(1, excludeFieldNames.length);
        assertEquals("charField", excludeFieldNames[0]);
    }

}
