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


import org.osgi.annotation.versioning.ConsumerType;

/**
 * A gauge metric is an instantaneous reading of a particular value. To instrument a queue's depth,
 * for example:<br>
 * <pre><code>
 * final Queue&lt;String&gt; queue = new ConcurrentLinkedQueue&lt;String&gt;();
 * final Gauge&lt;Integer&gt; queueDepth = new Gauge&lt;Integer&gt;() {
 *     public Integer getValue() {
 *         return queue.size();
 *     }
 * };
 * </code></pre>
 *
 * <p> A Gauge instance should be registered with OSGi ServiceRegistry with {@code Gauge#NAME} set
 * to Gauge name. Then the Gauge instance would be registered with MetricService via whiteboard
 * pattern
 *
 * @param <T> the type of the metric's value
 */
@ConsumerType
public interface Gauge<T> {
    /**
     * Service property name which determines the name of the Gauge
     */
    String NAME = "name";
    /**
     * Returns the metric's current value.
     *
     * @return the metric's current value
     */
    T getValue();
}