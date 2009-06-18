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
package org.apache.sling.servlets.resolver.internal.helper;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;

/**
 * The <code>WeightedResource</code> is a <code>Resource</code> which is
 * attributed with weight values to make it a <code>Comparable</code> to be
 * used to build the result collection returned by
 * {@link ResourceCollector#getServlets(Resource)}.
 * <p>
 * The order amongst <code>WeightedResource</code> instances is defined as
 * follows and implemented in the {@link #compareTo(WeightedResource)} method:
 * <ol>
 * <li>The instance with more matching selectors ({@link #getNumSelectors()})
 * is ordered before the instance with less matching selectors.</li>
 * <li>If the matching selectors are equal, the instance with the higher
 * method/prefix weight ({@link #getMethodPrefixWeight()}) is order before the
 * instance with a lower method/prefix weight.</li>
 * <li>Finally, if also the method/prefix weights are equal, the instance with
 * the lower ordinal number ({@link #getOrdinal()} is ordered before the
 * instance with the higher ordinal number.</li>
 * </ol>
 */
final class WeightedResource extends ResourceWrapper implements
        Comparable<WeightedResource> {

    /**
     * Weight value assigned to an instance just bearing request method name
     * (value is 0).
     */
    static final int WEIGHT_NONE = 0;

    /**
     * Weight value assigned to an instance if the the resource name neither
     * contains the parent resource name as a prefix, nor the request method
     * name nor the request extension (value is -1).
     */
    static final int WEIGHT_LAST_RESSORT = -1;

    /**
     * Weight value added to method/prefix weight if the resource name contains
     * the the name of the parent resource as its prefix (value is 1).
     */
    static final int WEIGHT_PREFIX = 1;

    /**
     * Weight value added to method/prefix weight if the resource name contains
     * the request extension (value is 2).
     */
    static final int WEIGHT_EXTENSION = 2;

    private final int ordinal;

    private final int numSelectors;

    private final int methodPrefixWeight;

    WeightedResource(int ordinal, Resource resource, int numSelectors,
            int methodPrefixWeight) {
        super(resource);

        this.ordinal = ordinal;
        this.numSelectors = numSelectors;
        this.methodPrefixWeight = methodPrefixWeight;
    }

    final public int getOrdinal() {
        return ordinal;
    }

    final public int getNumSelectors() {
        return numSelectors;
    }

    final public int getMethodPrefixWeight() {
        return methodPrefixWeight;
    }

    @Override
    final public int hashCode() {
        return ordinal;
    }

    @Override
    final public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    final public String toString() {
        return getClass().getSimpleName() + "[" + getOrdinal() + "]: "
            + getResource() + ", #selectors=" + getNumSelectors()
            + ", methodPrefixWeight=" + getMethodPrefixWeight();
    }

    final public int compareTo(WeightedResource o) {
        if (equals(o)) {
            return 0;
        }

        // compare by the number of selectors (more selectors wins)
        if (numSelectors > o.numSelectors) {
            return -1;
        } else if (numSelectors < o.numSelectors) {
            return 1;
        }

        // selectors are equal, check method/extension weight (higher wins)
        if (methodPrefixWeight > o.methodPrefixWeight) {
            return -1;
        } else if (methodPrefixWeight < o.methodPrefixWeight) {
            return 1;
        }

        // extensions are equal, compare ordinal (lower ordinal wins)
        return (ordinal < o.ordinal) ? -1 : 1;
    }
}
