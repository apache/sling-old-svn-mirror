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
package org.apache.sling.adapter.internal;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * The <code>AdapterFactoryDescriptorKey</code> provides the indexing
 * functionality for the {@link AdapterFactoryDescriptorMap}. The key consists
 * of the OSGi <code>service.id</code> of the
 * {@link org.apache.sling.api.adapter.AdapterFactory} service and the ID of
 * the the bundle providing the service.
 * <p>
 * Sort order among the keys is defined primarily by the bundle id and
 * secondarily by the service id.
 */
public class AdapterFactoryDescriptorKey implements
        Comparable<AdapterFactoryDescriptorKey> {

    private long bundleId;

    private long serviceId;

    public AdapterFactoryDescriptorKey(ServiceReference ref) {
        bundleId = ref.getBundle().getBundleId();
        serviceId = OsgiUtil.toLong(ref.getProperty(Constants.SERVICE_ID), -1);
    }

    public int compareTo(AdapterFactoryDescriptorKey o) {
        if (o.equals(this)) {
            return 0;
        }

        // result for differing bundleId
        if (bundleId < o.bundleId) {
            return -1;
        } else if (bundleId > o.bundleId) {
            return 1;
        }

        // result for differing serviceId, we do not expect the two
        // serviceId values to be equal because otherwise the equals
        // test above would have yielded true
        if (serviceId < o.serviceId) {
            return -1;
        }

        // serviceId is larger than the other object's, we do not expect
        // the two serviceId values to be equal because otherwise the equals
        // test above would have yielded true
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof AdapterFactoryDescriptorKey) {
            AdapterFactoryDescriptorKey oKey = (AdapterFactoryDescriptorKey) o;
            return bundleId == oKey.bundleId && serviceId == oKey.serviceId;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (int) (bundleId * 33 + serviceId);
    }
}
