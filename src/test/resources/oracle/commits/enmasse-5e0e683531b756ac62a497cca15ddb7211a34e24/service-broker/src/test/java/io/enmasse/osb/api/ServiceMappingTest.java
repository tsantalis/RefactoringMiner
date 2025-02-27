/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceMappingTest {
    @Test
    public void testRegexp() {
        String regexp = ServiceMapping.addressRegexp;
        assertTrue("foobar".matches(regexp));
        assertFalse("/foobar".matches(regexp));
        assertFalse("/foobar/".matches(regexp));
        assertFalse("foobar/".matches(regexp));
        assertTrue("foo.bar".matches(regexp));
        assertTrue("foo/bar".matches(regexp));
        assertTrue("foo/bar/baz".matches(regexp));
    }

}
