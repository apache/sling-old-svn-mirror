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
package org.apache.sling.testing.mock.osgi;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

import org.apache.sling.commons.osgi.ServiceUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Mock {@link ServiceReference} implementation.
 */
class MockServiceReference implements ServiceReference {

    private final Bundle bundle;
    private final MockServiceRegistration serviceRegistration;
    private volatile Comparable<Object> comparable;

    public MockServiceReference(final Bundle bundle, final MockServiceRegistration serviceRegistration) {
        this.bundle = bundle;
        this.serviceRegistration = serviceRegistration;
        this.comparable = buildComparable();
    }

    private Comparable<Object> buildComparable() {
        Map<String,Object> props = MapUtil.toMap(serviceRegistration.getProperties());
        return ServiceUtil.getComparableForServiceRanking(props);
    }

    @Override
    public Bundle getBundle() {
        return this.bundle;
    }

    /**
     * Set service reference property
     * @param key Key
     * @param value Value
     */
    public void setProperty(final String key, final Object value) {
        this.serviceRegistration.getProperties().put(key, value);
        this.comparable = buildComparable();
    }

    @Override
    public Object getProperty(final String key) {
        return this.serviceRegistration.getProperties().get(key);
    }

    @Override
    public String[] getPropertyKeys() {
        Dictionary<String, Object> props = this.serviceRegistration.getProperties();
        return Collections.list(props.keys()).toArray(new String[props.size()]);
    }

    @Override
    public int hashCode() {
        return comparable.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof MockServiceReference)) {
            return false;
        }
        return comparable.equals(((MockServiceReference)obj).comparable);
    }

    @Override
    public int compareTo(final Object obj) {
        if (!(obj instanceof MockServiceReference)) {
            return 0;
        }
        return comparable.compareTo(((MockServiceReference)obj).comparable);
    }

    long getServiceId() {
        Number serviceID = (Number) getProperty(Constants.SERVICE_ID);
        if (serviceID != null) {
            return serviceID.longValue();
        } else {
            return 0L;
        }
    }

    int getServiceRanking() {
        Number serviceRanking = (Number) getProperty(Constants.SERVICE_RANKING);
        if (serviceRanking != null) {
            return serviceRanking.intValue();
        } else {
            return 0;
        }
    }

    Object getService() {
        return this.serviceRegistration.getService();
    }

    // --- unsupported operations ---
    @Override
    public Bundle[] getUsingBundles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAssignableTo(final Bundle otherBundle, final String className) {
        throw new UnsupportedOperationException();
    }

}
