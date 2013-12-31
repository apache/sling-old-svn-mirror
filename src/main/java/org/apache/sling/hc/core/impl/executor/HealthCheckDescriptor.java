/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.core.impl.executor;

import org.apache.sling.hc.util.HealthCheckMetaData;
import org.osgi.framework.ServiceReference;

/**
 * Immutable class that holds meta information for a health check available via the result.
 */
public class HealthCheckDescriptor {

    private final HealthCheckMetaData metaData;
    private final transient ServiceReference serviceReference;

    public HealthCheckDescriptor(final ServiceReference healthCheckRef) {
        if (healthCheckRef == null) {
            throw new IllegalArgumentException("HealthCheck reference must not be null");
        }
        this.serviceReference = healthCheckRef;
        this.metaData = new HealthCheckMetaData(healthCheckRef);
    }

    @Override
    public String toString() {
        return "HealthCheck '" + this.metaData.getTitle() + "'";
    }

    public HealthCheckMetaData getMetaData() {
        return this.metaData;
    }

    public ServiceReference getServiceReference() {
        return this.serviceReference;
    }

    @Override
    public int hashCode() {
        return this.metaData.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if ( !(obj instanceof HealthCheckDescriptor)) {
            return false;
        }
        final HealthCheckDescriptor other = (HealthCheckDescriptor) obj;
        return this.metaData.equals(other.metaData);
    }
}
