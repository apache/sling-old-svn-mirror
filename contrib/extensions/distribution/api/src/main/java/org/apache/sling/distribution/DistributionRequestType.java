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
package org.apache.sling.distribution;

import aQute.bnd.annotation.ProviderType;

import javax.annotation.CheckForNull;

/**
 * The request type tied to a specific {@link org.apache.sling.distribution.DistributionRequest}, used to decide how
 * the distribution content should be aggregated.
 * <p/>
 * {@code ADD} requests can for example lead to the creation of a package of resources to be persisted on the target instance.
 * {@code DELETE} requests can for example lead to the creation of a "command package" to be sent to the target instance
 * to actually remove the resources specified in {@link DistributionRequest#getPaths()}.
 * {@code PULL} requests can for example lead to the creation of a "command package" that will trigger fetching of content
 * from the target instance.
 */
@ProviderType
public enum DistributionRequestType {

    /**
     * Action type for adding content
     */
    ADD,

    /**
     * Action type for deleting content
     */
    DELETE,

    /**
     * Action type for pulling content
     */
    PULL,

    /**
     * Action type for testing connection. No content is modified.
     */
    TEST;

    /**
     * Creates an action type for the given name. if the name cannot be mapped to a enum type or if
     * it's {@code null}, {@code null} is returned.
     *
     * @param n the name
     * @return the type or {@code null}
     */
    @CheckForNull
    public static DistributionRequestType fromName(String n) {
        if (n == null) {
            return null;
        }
        try {
            return DistributionRequestType.valueOf(n.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
