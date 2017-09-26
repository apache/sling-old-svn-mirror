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
package org.apache.sling.commons.scheduler.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Vector;

import org.junit.Test;

public class MetricsHelperTest {

    class T1{ }
    
    class T2{ }
    
    class Tother{ }
    
    private final class QSC implements QuartzSchedulerConfiguration {
        
        private Class<? extends Annotation> annotationType;
        private int slowThresholdMillis;
        private boolean useleaderforsingle;
        private String poolName;
        private String[] metrics_filters;
        private String[] allowedPoolNames;

        @Override
        public Class<? extends Annotation> annotationType() {
            return annotationType;
        }

        @Override
        public int slowThresholdMillis() {
            return slowThresholdMillis;
        }

        @Override
        public boolean scheduler_useleaderforsingle() {
            return useleaderforsingle;
        }

        @Override
        public String poolName() {
            return poolName;
        }

        @Override
        public String[] metrics_filters() {
            return metrics_filters;
        }

        @Override
        public String[] allowedPoolNames() {
            return allowedPoolNames;
        }
    }

    @Test
    public void testAsMetricsSuffix_NullValues() throws Exception {
        assertEquals(MetricsHelper.UNKNOWN_JOBNAME_SUFFIX ,MetricsHelper.asMetricsSuffix(null));
    }
    
    @Test
    public void testAsMetricsSuffix_Suffixes() throws Exception {
        assertEquals("bar", MetricsHelper.asMetricsSuffix("bar"));
        assertEquals("foo.1", MetricsHelper.asMetricsSuffix("foo.1"));
        assertEquals("abc.d.1", MetricsHelper.asMetricsSuffix("asd.bas.cdf.d.1"));
        assertEquals("abcd.ex.1", MetricsHelper.asMetricsSuffix("asd.bas.cdf.d.ex.1"));
        assertEquals("abcde.f.1", MetricsHelper.asMetricsSuffix("a.b.c.d.e.f.1"));

        assertEquals("abcdef..1", MetricsHelper.asMetricsSuffix("a.b.c.d.e...f....1..."));
        assertEquals("abcdef..1", MetricsHelper.asMetricsSuffix("a.b.c.d.e.f....1..."));
        assertEquals("abcdef..1", MetricsHelper.asMetricsSuffix("a.b.c.d.e.f.....1"));
        assertEquals("abcde.f.1", MetricsHelper.asMetricsSuffix("a.b.c.d.e.f.1...."));
        assertEquals("abcde.f.1", MetricsHelper.asMetricsSuffix("a.b...c..d.e......f.1"));
    }
    
    @Test
    public void testDeriveFilterName_NullValues() throws Exception {
        assertNull(MetricsHelper.deriveFilterName(null, null));
        assertNull(MetricsHelper.deriveFilterName(null, "a"));
        final QSC config = new QSC();
        ConfigHolder configHolder = new ConfigHolder(config);
        assertNull(MetricsHelper.deriveFilterName(configHolder, null));
        assertNull(MetricsHelper.deriveFilterName(configHolder, "a"));
        config.metrics_filters = new String[0];
        configHolder = new ConfigHolder(config);
        assertNull(MetricsHelper.deriveFilterName(configHolder, null));
        assertNull(MetricsHelper.deriveFilterName(configHolder, "a"));
        config.metrics_filters = new String[] {"unrelated", "foo.bar.Class"};
        configHolder = new ConfigHolder(config);
        assertNull(MetricsHelper.deriveFilterName(configHolder, null));
        assertNull(MetricsHelper.deriveFilterName(configHolder, "a"));
    }
    
    @Test
    public void testDeriveFilterName_FilterVariations() throws Exception {
        final QSC config = new QSC();
        config.metrics_filters = new String[] { 
                "filter1=org.apache.sling.commons.scheduler.impl.MetricsHelperTest",
                "filter2=org.apache.sling.commons.scheduler.impl.QuartzSchedulerTest",
                "filter3=org.apache.sling.commons.scheduler.impl.Tunused",
                "filter4=java.lang.String"
        };
        final ConfigHolder configHolder = new ConfigHolder(config);
        assertEquals("filter1", MetricsHelper.deriveFilterName(configHolder, new MetricsHelperTest()));
        assertEquals("filter1", MetricsHelper.deriveFilterName(configHolder, new T1()));
        assertEquals("filter1", MetricsHelper.deriveFilterName(configHolder, new T2()));
        assertEquals("filter1", MetricsHelper.deriveFilterName(configHolder, new Tother()));
        assertEquals("filter2", MetricsHelper.deriveFilterName(configHolder, new QuartzSchedulerTest()));
        assertEquals("filter4", MetricsHelper.deriveFilterName(configHolder, new String()));
        assertEquals(null, MetricsHelper.deriveFilterName(configHolder, new StringBuffer()));
        assertEquals(null, MetricsHelper.deriveFilterName(configHolder, new TopologyHandlerTest()));
        assertEquals(null, MetricsHelper.deriveFilterName(configHolder, new Vector<Object>()));

        assertEquals(null, MetricsHelper.deriveFilterName(configHolder, null));
    }
    
    @Test
    public void testConfigHolder() throws Exception {
        final QSC config = new QSC();
        config.metrics_filters = new String[] { 
                "filter1=org.apache.sling.commons.scheduler.impl.MetricsHelperTest",
                "filter2=org.apache.sling.commons.scheduler.impl.QuartzSchedulerTest",
                "filter3=org.apache.sling.commons.scheduler.impl.Tunused",
                "filter4=java.lang.String",
                "wrongFilter5=java.foo.Wrong$1",
                "wrongFilter6=java.bar.Wrong*",
                "wrongFilter7=java.zet.Wrong?"
        };
        final ConfigHolder configHolder = new ConfigHolder(config);
        assertTrue(configHolder.getFilterDefinitions().containsKey("java.lang"));
        assertTrue(configHolder.getFilterDefinitions().containsKey("org.apache.sling.commons.scheduler.impl"));
        assertEquals(2, configHolder.getFilterDefinitions().size());
        assertEquals(3, configHolder.getFilterDefinitions().get("org.apache.sling.commons.scheduler.impl").size());
    }
}
