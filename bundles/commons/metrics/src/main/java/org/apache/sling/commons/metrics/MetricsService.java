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

package org.apache.sling.commons.metrics;

import aQute.bnd.annotation.ProviderType;

/**
 * The {@code MetricsService} enables creation of various types of {@code Metric}.
 */
@ProviderType
public interface MetricsService {
    /**
     * Dummy variant of MetricsService which does not
     * collect any metric
     */
    MetricsService NOOP = new MetricsService() {
        @Override
        public Timer timer(String name) {
            return NoopMetric.INSTANCE;
        }

        @Override
        public Histogram histogram(String name) {
            return NoopMetric.INSTANCE;
        }

        @Override
        public Counter counter(String name) {
            return NoopMetric.INSTANCE;
        }

        @Override
        public Meter meter(String name) {
            return NoopMetric.INSTANCE;
        }

        @Override
        public <A> A adaptTo(Class<A> type) {
            return null;
        }
    };

    /**
     * Creates a new {@link Timer} and registers it under the given name.
     * If a timer with same name exists then same instance is returned
     *
     * @param name the name of the metric
     * @return a new {@link Timer}
     */
    Timer timer(String name);

    /**
     * Creates a new {@link Histogram} and registers it under the given name.
     * If a histogram with same name exists then same instance is returned.
     *
     * @param name the name of the metric
     * @return a new {@link Histogram}
     */
    Histogram histogram(String name);

    /**
     * Creates a new {@link Counter} and registers it under the given name.
     * If a counter with same name exists then same instance is returned
     *
     * @param name the name of the metric
     * @return a new {@link Counter}
     */
    Counter counter(String name);

    /**
     * Creates a new {@link Meter} and registers it under the given name.
     * If a meter with same name exists then same instance is returned
     *
     * @param name the name of the metric
     * @return a new {@link Meter}
     */
    Meter meter(String name);

    /**
     * Adapts the service to the specified type. This can be used to
     * get instance to underlying {@code MetricRegistry}
     *
     * @param <A> The type to which this metric is to be adapted.
     * @param type Class object for the type to which this metric is to be adapted.
     * @return The object, of the specified type, to which this metric has been adapted
     * or null if this metric cannot be adapted to the specified type.
     */
    <A> A adaptTo(Class<A> type);
}
