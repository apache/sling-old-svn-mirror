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
package org.apache.sling.servlet.resolver.helper;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;

public class LocationResource extends ResourceWrapper implements Comparable<LocationResource> {

    static final int WEIGHT_NONE = 0;

    static final int WEIGHT_LAST_RESSORT = -1;

    static final int WEIGHT_PREFIX = 1;

    static final int WEIGHT_EXTENSION = 2;

    private final int ordinal;

    private final int numSelectors;

    private int methodPrefixWeight;

    public LocationResource(int ordinal, Resource resource, int numSelectors,
            int methodPrefixWeight) {
        super(resource);
        
        this.ordinal = ordinal;
        this.numSelectors = numSelectors;
        this.methodPrefixWeight = methodPrefixWeight;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public int getNumSelectors() {
        return numSelectors;
    }

    public int getMethodPrefixWeight() {
        return methodPrefixWeight;
    }

    @Override
    public int hashCode() {
        return ordinal;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getOrdinal() + "]: "
            + getResource() + ", #selectors=" + getNumSelectors()
            + ", methodPrefixWeight=" + getMethodPrefixWeight();
    }

    public int compareTo(LocationResource o) {
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
