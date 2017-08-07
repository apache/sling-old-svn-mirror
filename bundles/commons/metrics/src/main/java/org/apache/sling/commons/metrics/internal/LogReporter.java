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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component(service = {}, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = LogReporter.Config.class, factory = true)
public class LogReporter implements ServiceTrackerCustomizer<MetricRegistry, Slf4jReporter> {

    private BundleContext context;
    private ServiceTracker<MetricRegistry, Slf4jReporter> tracker;
    private Config config;

    @Activate
    protected void activate(Config config, BundleContext context) {
        this.config = config;
        this.context = context;
        tracker = new ServiceTracker<>(context, MetricRegistry.class, this);
        tracker.open();
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        tracker.close();
    }

    //~---------------------------------------------< ServiceTracker >

    @Override
    public Slf4jReporter addingService(ServiceReference<MetricRegistry> serviceReference) {
        MetricRegistry registry = context.getService(serviceReference);
        String metricRegistryName = (String) serviceReference.getProperty(MetricWebConsolePlugin.METRIC_REGISTRY_NAME);

        if (config.registryName() == null || config.registryName().length() == 0
                || config.registryName().equals(metricRegistryName)) {
            Slf4jReporter.Builder builder = Slf4jReporter.forRegistry(registry).
                    outputTo(LoggerFactory.getLogger(config.loggerName())).
                    withLoggingLevel(config.level());

            if (config.prefix() != null && config.prefix().length() > 0) {
                builder.filter(new PrefixFilter(config.prefix()));
            } else if (config.pattern() != null && config.pattern().length() > 0) {
                builder.filter(new PatternFilter(config.pattern()));
            }

            Slf4jReporter reporter = builder.build();
            reporter.start(config.period(), config.timeUnit());
            return reporter;
        } else {
            return null;
        }
    }

    @Override
    public void modifiedService(ServiceReference<MetricRegistry> serviceReference, Slf4jReporter reporter) {
        // NO OP
    }

    @Override
    public void removedService(ServiceReference<MetricRegistry> serviceReference, Slf4jReporter reporter) {
        if (reporter != null) {
            reporter.close();
        }
    }

    private class PrefixFilter implements MetricFilter {
        private final String prefix;

        private PrefixFilter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean matches(String s, Metric metric) {
            return s.startsWith(prefix);
        }
    }

    private class PatternFilter implements MetricFilter {
        private final Pattern pattern;

        private PatternFilter(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }

        @Override
        public boolean matches(String s, Metric metric) {
            return pattern.matcher(s).matches();
        }
    }

    @ObjectClassDefinition(name = "Apache Sling Metrics Log Reporter Configuration")
    @interface Config {

        @AttributeDefinition(description = "Period at which the metrics data will be logged")
        long period() default 5;

        @AttributeDefinition(description = "Unit of time for evaluating the period")
        TimeUnit timeUnit() default TimeUnit.MINUTES;

        @AttributeDefinition(description = "The log level to log at.")
        Slf4jReporter.LoggingLevel level() default Slf4jReporter.LoggingLevel.INFO;

        @AttributeDefinition(description = "The logger name")
        String loggerName() default "metrics";

        @AttributeDefinition(description = "If specified, only metrics whose name starts with this value are logged. If both prefix and pattern are set, prefix is used.")
        String prefix() default "";

        @AttributeDefinition(description = "If specified, only metrics whose name matches this regular expression will be logged. If both prefix and pattern are set, prefix is used.")
        String pattern() default "";

        @AttributeDefinition(description = "Restrict the metrics logged to a specifically named registry.")
        String registryName() default "";
    }

}
