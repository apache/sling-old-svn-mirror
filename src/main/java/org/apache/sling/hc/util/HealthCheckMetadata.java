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
package org.apache.sling.hc.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.hc.api.HealthCheck;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;

/**
 * This class helps retrieving meta data information about a health check service.
 * @since 1.1
 */
public class HealthCheckMetadata {

    private final String name;

    private final String title;

    private final long serviceId;

    private final List<String> tags;

    public HealthCheckMetadata(final ServiceReference ref) {
        this.serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
        this.name = (String) ref.getProperty(HealthCheck.NAME);
        this.title = getHealthCheckTitle(ref);
        this.tags = arrayPropertyToListOfStr(ref.getProperty(HealthCheck.TAGS));

    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }


    public List<String> getTags() {
        return tags;
    }

    public long getServiceId() {
        return this.serviceId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (serviceId ^ (serviceId >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if ( !(obj instanceof HealthCheckMetadata)) {
            return false;
        }
        final HealthCheckMetadata other = (HealthCheckMetadata) obj;
        return serviceId == other.serviceId;
    }

    @Override
    public String toString() {
        return "HealthCheck '" + name + "'";
    }

    private String getHealthCheckTitle(final ServiceReference ref) {
        String name = (String) ref.getProperty(HealthCheck.NAME);
        if (StringUtils.isBlank(name)) {
            name = (String) ref.getProperty(Constants.SERVICE_DESCRIPTION);
        }
        if (StringUtils.isBlank(name)) {
            name = (String) ref.getProperty(ComponentConstants.COMPONENT_NAME);
        }
        return name;
    }

    private List<String> arrayPropertyToListOfStr(final Object arrayProp) {
        List<String> res = new LinkedList<String>();
        if (arrayProp instanceof String) {
            res.add((String) arrayProp);
        } else if (arrayProp instanceof String[]) {
            res.addAll(Arrays.asList((String[]) arrayProp));
        }
        return res;
    }
}
