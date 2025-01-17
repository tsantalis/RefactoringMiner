/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.KubeUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class KubeUtilTest {
    @Test
    public void testLeaveSpaceForPodIdentifier() {
        String address = "receiver-round-robincli_rhearatherlongaddresswhichcanbeverylongblablabla";
        String id = KubeUtil.sanitizeName(address);
        assertThat(id.length(), is(60));
        String id2 = KubeUtil.sanitizeName(id);
        assertThat(id, is(id2));
    }
}
