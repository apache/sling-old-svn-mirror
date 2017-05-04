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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InternalQueueConfigurationTest {

    private InternalQueueConfiguration.Config createConfig(final double maxParallel) {
        return createConfig(null, "QueueConfigurationTest", maxParallel);
    }

    private InternalQueueConfiguration.Config createConfig(final String[] topics) {
        return createConfig(topics, "QueueConfigurationTest", ConfigurationConstants.DEFAULT_MAX_PARALLEL);
    }

    private InternalQueueConfiguration.Config createConfig(final String[] topics, final String name) {
        return createConfig(topics, name, ConfigurationConstants.DEFAULT_MAX_PARALLEL);
    }

    private InternalQueueConfiguration.Config createConfig(final String[] topics,
            final String name,
            final double maxParallel) {
        return new InternalQueueConfiguration.Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return InternalQueueConfiguration.Config.class;
            }

            @Override
            public String queue_name() {
                return name;
            }

            @Override
            public String[] queue_topics() {
                return topics;
            }

            @Override
            public String queue_type() {
                return "UNORDERED";
            }

            @Override
            public String queue_priority() {
                return ConfigurationConstants.DEFAULT_PRIORITY;
            }

            @Override
            public int queue_retries() {
                return ConfigurationConstants.DEFAULT_RETRIES;
            }

            @Override
            public long queue_retrydelay() {
                return ConfigurationConstants.DEFAULT_RETRY_DELAY;
            }

            @Override
            public double queue_maxparallel() {
                return maxParallel;
            }

            @Override
            public boolean queue_keepJobs() {
                return false;
            }

            @Override
            public boolean queue_preferRunOnCreationInstance() {
                return false;
            }

            @Override
            public int queue_threadPoolSize() {
                return 0;
            }

            @Override
            public int service_ranking() {
                return 0;
            }
        };
    }

    @org.junit.Test public void testMaxParallel() {
        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(-1));
        assertEquals(Runtime.getRuntime().availableProcessors(), c.getMaxParallel());

        // Edge cases 0.0 and 1.0 (treated as int numbers)
        c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(0.0));
        assertEquals(0, c.getMaxParallel());

        c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(1.0));
        assertEquals(1, c.getMaxParallel());

        // percentage (50%)
        c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(0.5));
        assertEquals((int) Math.round(Runtime.getRuntime().availableProcessors() * 0.5), c.getMaxParallel());

        // rounding
        c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(0.90));
        assertEquals((int) Math.round(Runtime.getRuntime().availableProcessors() * 0.9), c.getMaxParallel());

        c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(0.99));
        assertEquals((int) Math.round(Runtime.getRuntime().availableProcessors() * 0.99), c.getMaxParallel());

        // Percentages can't go over 99% (0.99)
        c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(1.01));
        assertEquals(Runtime.getRuntime().availableProcessors(), c.getMaxParallel());

        // Treat negative values same a -1 (all cores)
        c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(-0.5));
        assertEquals(Runtime.getRuntime().availableProcessors(), c.getMaxParallel());

        c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(-2));
        assertEquals(Runtime.getRuntime().availableProcessors(), c.getMaxParallel());
    }

    @org.junit.Test public void testTopicMatchersDot() {
        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(new String[] {"a."}));
        assertTrue(c.isValid());
        assertNotNull(c.match("a/b"));
        assertNotNull(c.match("a/c"));
        assertNull(c.match("a"));
        assertNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatchersStar() {
        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(new String[] {"a*"}));
        assertTrue(c.isValid());
        assertNotNull(c.match("a/b"));
        assertNotNull(c.match("a/c"));
        assertNull(c.match("a"));
        assertNotNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatchers() {
        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(new String[] {"a"}));
        assertTrue(c.isValid());
        assertNull(c.match("a/b"));
        assertNull(c.match("a/c"));
        assertNotNull(c.match("a"));
        assertNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatcherAndReplacement() {
        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(new String[] {"a."}, "test-queue-{0}"));
        assertTrue(c.isValid());
        final String b = "a/b";
        assertNotNull(c.match(b));
        assertEquals("test-queue-b", c.match(b));
        final String d = "a/d";
        assertNotNull(c.match(d));
        assertEquals("test-queue-d", c.match(d));
    }

    @org.junit.Test public void testTopicMatchersDotAndSlash() {
        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(new String[] {"a/."}));
        assertTrue(c.isValid());
        assertNotNull(c.match("a/b"));
        assertNotNull(c.match("a/c"));
        assertNull(c.match("a"));
        assertNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatchersStarAndSlash() {
        final Map<String, Object> p = new HashMap<>();
        p.put(ConfigurationConstants.PROP_TOPICS, new String[] {"a/*"});
        p.put(ConfigurationConstants.PROP_NAME, "test");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(new String[] {"a/*"}));
        assertTrue(c.isValid());
        assertNotNull(c.match("a/b"));
        assertNotNull(c.match("a/c"));
        assertNull(c.match("a"));
        assertNotNull(c.match("a/b/c"));
        assertNull(c.match("t"));
        assertNull(c.match("t/x"));
    }

    @org.junit.Test public void testTopicMatcherAndReplacementAndSlash() {
        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(new String[] {"a/."}, "test-queue-{0}"));
        assertTrue(c.isValid());
        final String b = "a/b";
        assertNotNull(c.match(b));
        assertEquals("test-queue-b", c.match(b));
        final String d = "a/d";
        assertNotNull(c.match(d));
        assertEquals("test-queue-d", c.match(d));
    }

    @org.junit.Test public void testNoTopicMatchers() {
        final Map<String, Object> p = new HashMap<>();
        p.put(ConfigurationConstants.PROP_NAME, "test");

        InternalQueueConfiguration c = InternalQueueConfiguration.fromConfiguration(Collections.<String, Object>emptyMap(), createConfig(null));
        assertFalse(c.isValid());
    }
}
