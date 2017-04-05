/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.event.impl.jobs.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

public class InternalQueueConfigurationTest {

    @org.junit.Test public void testMaxParallel() {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(ConfigurationConstants.PROP_NAME, "QueueConfigurationTest");
        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, -1);

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals(Runtime.getRuntime().availableProcessors(), c.getMaxParallel());

        // Edge cases 0.0 and 1.0 (treated as int numbers)
        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, 0.0);
        c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals(0, c.getMaxParallel());

        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, 1.0);
        c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals(1, c.getMaxParallel());

        // percentage (50%)
        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, 0.5);
        c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals((int) Math.round(Runtime.getRuntime().availableProcessors() * 0.5), c.getMaxParallel());

        // rounding
        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, 0.90);
        c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals((int) Math.round(Runtime.getRuntime().availableProcessors() * 0.9), c.getMaxParallel());

        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, 0.99);
        c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals((int) Math.round(Runtime.getRuntime().availableProcessors() * 0.99), c.getMaxParallel());

        // Percentages can't go over 99% (0.99)
        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, 1.01);
        c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals(Runtime.getRuntime().availableProcessors(), c.getMaxParallel());

        // Treat negative values same a -1 (all cores)
        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, -0.5);
        c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals(Runtime.getRuntime().availableProcessors(), c.getMaxParallel());

        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, -2);
        c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals(Runtime.getRuntime().availableProcessors(), c.getMaxParallel());

        // Invalid number results in ConfigurationConstants.DEFAULT_MAX_PARALLEL
        p.put(ConfigurationConstants.PROP_MAX_PARALLEL, "a string");
        c = InternalQueueConfiguration.fromConfiguration(p);
        assertEquals(ConfigurationConstants.DEFAULT_MAX_PARALLEL, c.getMaxParallel());
    }

    @org.junit.Test public void testTopicMatchersDot() {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(ConfigurationConstants.PROP_TOPICS, new String[] {"a."});
        p.put(ConfigurationConstants.PROP_NAME, "test");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(p);
        assertTrue(c.isValid());
        assertNotNull(c.match("a/b"));
        assertNotNull(c.match("a/c"));
        assertNull(c.match("a"));
        assertNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatchersStar() {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(ConfigurationConstants.PROP_TOPICS, new String[] {"a*"});
        p.put(ConfigurationConstants.PROP_NAME, "test");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(p);
        assertTrue(c.isValid());
        assertNotNull(c.match("a/b"));
        assertNotNull(c.match("a/c"));
        assertNull(c.match("a"));
        assertNotNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatchers() {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(ConfigurationConstants.PROP_TOPICS, new String[] {"a"});
        p.put(ConfigurationConstants.PROP_NAME, "test");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(p);
        assertTrue(c.isValid());
        assertNull(c.match("a/b"));
        assertNull(c.match("a/c"));
        assertNotNull(c.match("a"));
        assertNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatcherAndReplacement() {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(ConfigurationConstants.PROP_TOPICS, new String[] {"a."});
        p.put(ConfigurationConstants.PROP_NAME, "test-queue-{0}");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(p);
        assertTrue(c.isValid());
        final String b = "a/b";
        assertNotNull(c.match(b));
        assertEquals("test-queue-b", c.match(b));
        final String d = "a/d";
        assertNotNull(c.match(d));
        assertEquals("test-queue-d", c.match(d));
    }

    @org.junit.Test public void testTopicMatchersDotAndSlash() {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(ConfigurationConstants.PROP_TOPICS, new String[] {"a/."});
        p.put(ConfigurationConstants.PROP_NAME, "test");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(p);
        assertTrue(c.isValid());
        assertNotNull(c.match("a/b"));
        assertNotNull(c.match("a/c"));
        assertNull(c.match("a"));
        assertNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatchersStarAndSlash() {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(ConfigurationConstants.PROP_TOPICS, new String[] {"a/*"});
        p.put(ConfigurationConstants.PROP_NAME, "test");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(p);
        assertTrue(c.isValid());
        assertNotNull(c.match("a/b"));
        assertNotNull(c.match("a/c"));
        assertNull(c.match("a"));
        assertNotNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatcherAndReplacementAndSlash() {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(ConfigurationConstants.PROP_TOPICS, new String[] {"a/."});
        p.put(ConfigurationConstants.PROP_NAME, "test-queue-{0}");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(p);
        assertTrue(c.isValid());
        final String b = "a/b";
        assertNotNull(c.match(b));
        assertEquals("test-queue-b", c.match(b));
        final String d = "a/d";
        assertNotNull(c.match(d));
        assertEquals("test-queue-d", c.match(d));
    }

    @org.junit.Test public void testNoTopicMatchers() {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(ConfigurationConstants.PROP_NAME, "test");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(p);
        assertFalse(c.isValid());
    }
}
