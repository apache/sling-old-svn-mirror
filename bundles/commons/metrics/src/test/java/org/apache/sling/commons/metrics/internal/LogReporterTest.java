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
package org.apache.sling.commons.metrics.internal;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import junitx.util.PrivateAccessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;


import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class LogReporterTest {

    @Mock
    private BundleContext bundleContext;

    LogReporter reporterService = new LogReporter();

    @Test
    public void testSpecificRegistryNameInclude() {
        MetricRegistry registry = new MetricRegistry();
        ServiceReference<MetricRegistry> registryServiceReference = mock(ServiceReference.class);
        when(bundleContext.getService(registryServiceReference)).thenReturn(registry);
        when(registryServiceReference.getProperty(MetricWebConsolePlugin.METRIC_REGISTRY_NAME)).thenReturn("oak");

        LogReporter.Config config = createConfigWithRegistryName("oak");
        reporterService.activate(config, bundleContext);

        Slf4jReporter reporter = null;
        try {
            reporter = reporterService.addingService(registryServiceReference);
            assertNotNull(reporter);
        } finally {
            if (reporter != null) {
                reporter.close();
            }
            reporterService.deactivate(bundleContext);
        }
    }

    @Test
    public void testSpecificRegistryNameExclude() {
        MetricRegistry registry = new MetricRegistry();
        ServiceReference<MetricRegistry> registryServiceReference = mock(ServiceReference.class);
        when(bundleContext.getService(registryServiceReference)).thenReturn(registry);
        when(registryServiceReference.getProperty(MetricWebConsolePlugin.METRIC_REGISTRY_NAME)).thenReturn("other");

        LogReporter.Config config = createConfigWithRegistryName("oak");
        reporterService.activate(config, bundleContext);

        Slf4jReporter reporter = null;
        try {
            reporter = reporterService.addingService(registryServiceReference);
            assertNull(reporter);
        } finally {
            if (reporter != null) {
                reporter.close();
            }
            reporterService.deactivate(bundleContext);
        }
    }

    @Test
    public void testSpecificRegistryNameExcludeNullName() {
        MetricRegistry registry = new MetricRegistry();
        ServiceReference<MetricRegistry> registryServiceReference = mock(ServiceReference.class);
        when(bundleContext.getService(registryServiceReference)).thenReturn(registry);

        LogReporter.Config config = createConfigWithRegistryName("oak");
        reporterService.activate(config, bundleContext);

        Slf4jReporter reporter = null;
        try {
            reporter = reporterService.addingService(registryServiceReference);
            assertNull(reporter);
        } finally {
            if (reporter != null) {
                reporter.close();
            }
            reporterService.deactivate(bundleContext);
        }
    }

    @Test
    public void testLoggerName() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        ServiceReference<MetricRegistry> registryServiceReference = mock(ServiceReference.class);
        when(bundleContext.getService(registryServiceReference)).thenReturn(registry);

        LogReporter.Config config = createConfigWithLoggerNameAndLevel("test", Slf4jReporter.LoggingLevel.WARN);
        reporterService.activate(config, bundleContext);

        Slf4jReporter reporter = null;
        try {
            reporter = reporterService.addingService(registryServiceReference);
            assertNotNull(reporter);

            Object loggerProxy = PrivateAccessor.getField(reporter, "loggerProxy");
            assertEquals("WarnLoggerProxy", loggerProxy.getClass().getSimpleName());

            Logger logger = (Logger) PrivateAccessor.getField(loggerProxy, "logger");
            assertEquals("test", logger.getName());
        } finally {
            if (reporter != null) {
                reporter.close();
            }
            reporterService.deactivate(bundleContext);
        }
    }

    @Test
    public void testPrefix() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        ServiceReference<MetricRegistry> registryServiceReference = mock(ServiceReference.class);
        when(bundleContext.getService(registryServiceReference)).thenReturn(registry);

        LogReporter.Config config = createConfigWithPrefix("testPrefix");
        reporterService.activate(config, bundleContext);

        Slf4jReporter reporter = null;
        try {
            reporter = reporterService.addingService(registryServiceReference);
            assertNotNull(reporter);

            MetricFilter filter = (MetricFilter) PrivateAccessor.getField(reporter, "filter");
            assertEquals("PrefixFilter", filter.getClass().getSimpleName());
            assertTrue(filter.matches("testPrefixedName", null));
            assertFalse(filter.matches("testNonPrefixedName", null));
        } finally {
            if (reporter != null) {
                reporter.close();
            }
            reporterService.deactivate(bundleContext);
        }
    }

    @Test
    public void testPattern() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        ServiceReference<MetricRegistry> registryServiceReference = mock(ServiceReference.class);
        when(bundleContext.getService(registryServiceReference)).thenReturn(registry);

        LogReporter.Config config = createConfigWithPattern("[0-9]test.*");
        reporterService.activate(config, bundleContext);

        Slf4jReporter reporter = null;
        try {
            reporter = reporterService.addingService(registryServiceReference);
            assertNotNull(reporter);

            MetricFilter filter = (MetricFilter) PrivateAccessor.getField(reporter, "filter");
            assertEquals("PatternFilter", filter.getClass().getSimpleName());
            assertTrue(filter.matches("5testTest", null));
            assertFalse(filter.matches("ZtestTest", null));
        } finally {
            if (reporter != null) {
                reporter.close();
            }
            reporterService.deactivate(bundleContext);
        }
    }

    @Test
    public void testPrefixAndPattern() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        ServiceReference<MetricRegistry> registryServiceReference = mock(ServiceReference.class);
        when(bundleContext.getService(registryServiceReference)).thenReturn(registry);

        LogReporter.Config config = createConfigWithPrefixAndPattern("testPrefix", "[0-9]test.*");
        reporterService.activate(config, bundleContext);

        Slf4jReporter reporter = null;
        try {
            reporter = reporterService.addingService(registryServiceReference);
            assertNotNull(reporter);

            MetricFilter filter = (MetricFilter) PrivateAccessor.getField(reporter, "filter");
            assertEquals("PrefixFilter", filter.getClass().getSimpleName());
            assertTrue(filter.matches("testPrefixedName", null));
            assertFalse(filter.matches("testNonPrefixedName", null));
        } finally {
            if (reporter != null) {
                reporter.close();
            }
            reporterService.deactivate(bundleContext);
        }
    }

    @Test
    public void testRemove() {
        Slf4jReporter reporter = mock(Slf4jReporter.class);
        reporterService.removedService(null, reporter);
        verify(reporter, times(1)).close();
    }

    @Test
    public void testNoOpCalls() {
        // extra no-op calls for coverage
        reporterService.removedService(null, null);
        reporterService.modifiedService(null, null);
    }

    private LogReporter.Config createConfigWithRegistryName(final String registryName) {
        return new LogReporter.Config() {
            @Override
            public long period() {
                return 5;
            }

            @Override
            public TimeUnit timeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public Slf4jReporter.LoggingLevel level() {
                return Slf4jReporter.LoggingLevel.INFO;
            }

            @Override
            public String loggerName() {
                return "metrics";
            }

            @Override
            public String prefix() {
                return null;
            }

            @Override
            public String pattern() {
                return null;
            }

            @Override
            public String registryName() {
                return registryName;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return LogReporter.Config.class;
            }
        };
    }

    private LogReporter.Config createConfigWithLoggerNameAndLevel(final String loggerName, final Slf4jReporter.LoggingLevel level) {
        return new LogReporter.Config() {
            @Override
            public long period() {
                return 5;
            }

            @Override
            public TimeUnit timeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public Slf4jReporter.LoggingLevel level() {
                return level;
            }

            @Override
            public String loggerName() {
                return loggerName;
            }

            @Override
            public String prefix() {
                return null;
            }

            @Override
            public String pattern() {
                return null;
            }

            @Override
            public String registryName() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return LogReporter.Config.class;
            }
        };
    }

    private LogReporter.Config createConfigWithPrefix(final String prefix) {
        return new LogReporter.Config() {
            @Override
            public long period() {
                return 5;
            }

            @Override
            public TimeUnit timeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public Slf4jReporter.LoggingLevel level() {
                return Slf4jReporter.LoggingLevel.INFO;
            }

            @Override
            public String loggerName() {
                return "metrics";
            }

            @Override
            public String prefix() {
                return prefix;
            }

            @Override
            public String pattern() {
                return null;
            }

            @Override
            public String registryName() {
                return null;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return LogReporter.Config.class;
            }
        };
    }

    private LogReporter.Config createConfigWithPattern(final String pattern) {
        return new LogReporter.Config() {
            @Override
            public long period() {
                return 5;
            }

            @Override
            public TimeUnit timeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public Slf4jReporter.LoggingLevel level() {
                return Slf4jReporter.LoggingLevel.INFO;
            }

            @Override
            public String loggerName() {
                return "metrics";
            }

            @Override
            public String prefix() {
                return null;
            }

            @Override
            public String pattern() {
                return pattern;
            }

            @Override
            public String registryName() {
                return null;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return LogReporter.Config.class;
            }
        };
    }

    private LogReporter.Config createConfigWithPrefixAndPattern(final String prefix, final String pattern) {
        return new LogReporter.Config() {
            @Override
            public long period() {
                return 5;
            }

            @Override
            public TimeUnit timeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public Slf4jReporter.LoggingLevel level() {
                return Slf4jReporter.LoggingLevel.INFO;
            }

            @Override
            public String loggerName() {
                return "metrics";
            }

            @Override
            public String prefix() {
                return prefix;
            }

            @Override
            public String pattern() {
                return pattern;
            }

            @Override
            public String registryName() {
                return null;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return LogReporter.Config.class;
            }
        };
    }
}
