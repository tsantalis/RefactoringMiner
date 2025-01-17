/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests related to topic wildcards matches
 */
public class TopicMatcherTest {

    @Test
    public void testFixedTopicMatch() {

        String subTopic = "mytopic/foo";
        String pubTopic = "mytopic/foo";

        assertTrue(TopicMatcher.isMatch(subTopic, pubTopic));
    }

    @Test
    public void testFixedTopicNotMatch() {

        String subTopic = "mytopic/foo";
        String pubTopic = "mytopic/bar";

        assertFalse(TopicMatcher.isMatch(subTopic, pubTopic));
    }

    @Test
    public void testTopicSharpWildcardMatch() {

        String subTopic = "mytopic/#";
        String pubTopic = "mytopic/foo";

        assertTrue(TopicMatcher.isMatch(subTopic, pubTopic));

        pubTopic = "mytopic/foo/bar";

        assertTrue(TopicMatcher.isMatch(subTopic, pubTopic));
    }

    @Test
    public void testTopicSharpWildcardNotMatch() {

        String subTopic = "mytopic/#";
        String pubTopic = "mytopic";

        assertFalse(TopicMatcher.isMatch(subTopic, pubTopic));
    }

    @Test
    public void testTopicPlusWildcardMatch() {

        String subTopic = "mytopic/+/bar";
        String pubTopic = "mytopic/foo/bar";

        assertTrue(TopicMatcher.isMatch(subTopic, pubTopic));

        pubTopic = "mytopic/another/bar";

        assertTrue(TopicMatcher.isMatch(subTopic, pubTopic));

        subTopic = "mytopic/foo/+";
        pubTopic = "mytopic/foo/bar";

        assertTrue(TopicMatcher.isMatch(subTopic, pubTopic));

        subTopic = "mytopic/+/bar/+/foo";
        pubTopic = "mytopic/a/bar/b/foo";

        assertTrue(TopicMatcher.isMatch(subTopic, pubTopic));
    }

    @Test
    public void testTopicPlusWildcardNotMatch() {

        String subTopic = "mytopic/+/bar";
        String pubTopic = "mytopic/bar";

        assertFalse(TopicMatcher.isMatch(subTopic, pubTopic));

        subTopic = "mytopic/foo/+";
        pubTopic = "mytopic/foo";

        assertFalse(TopicMatcher.isMatch(subTopic, pubTopic));

        subTopic = "mytopic/+/bar/+/foo";
        pubTopic = "mytopic/a/bar/b/foo/c";

        assertFalse(TopicMatcher.isMatch(subTopic, pubTopic));
    }
}
