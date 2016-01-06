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
public interface Metric {
    /**
     * Adapts the Metric to the specified type.
     *
     * @param <A> The type to which this metric is to be adapted.
     * @param type Class object for the type to which this metric is to be adapted.
     * @return The object, of the specified type, to which this metric has been adapted
     * or null if this metric cannot be adapted to the specified type.
     */
    <A> A adaptTo(Class<A> type);
}
