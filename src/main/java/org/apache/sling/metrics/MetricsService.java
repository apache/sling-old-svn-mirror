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

package org.apache.sling.metrics;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface MetricsService {
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
    };

    /**
     * Creates a new {@link com.codahale.metrics.Timer} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link com.codahale.metrics.Timer}
     */
    Timer timer(String name);

    /**
     * Creates a new {@link com.codahale.metrics.Histogram} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link com.codahale.metrics.Histogram}
     */
    Histogram histogram(String name);

    /**
     * Creates a new {@link com.codahale.metrics.Counter} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link com.codahale.metrics.Counter}
     */
    Counter counter(String name);

    /**
     * Creates a new {@link com.codahale.metrics.Meter} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link com.codahale.metrics.Meter}
     */
    Meter meter(String name);
}
