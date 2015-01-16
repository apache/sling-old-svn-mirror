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
package org.apache.sling.distribution.transport.impl;

import java.net.URI;

/**
 * A distribution endpoint
 */
public class DistributionEndpoint {

    private final URI uri;

    public DistributionEndpoint(URI uri) {
        this.uri = uri;
    }

    public DistributionEndpoint(String uriString) {
        this.uri = URI.create(uriString);
    }

    /**
     * get the URI of this distribution endpoint
     *
     * @return a <code>URI</code>
     */
    public URI getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri.toASCIIString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistributionEndpoint that = (DistributionEndpoint) o;

        return uri.equals(that.uri);

    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}
