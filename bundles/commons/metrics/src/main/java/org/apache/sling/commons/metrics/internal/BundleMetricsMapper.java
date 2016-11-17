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

package org.apache.sling.commons.metrics.internal;

import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ObjectNameFactory;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BundleMetricsMapper implements ObjectNameFactory{
    public static final String HEADER_DOMAIN_NAME = "Sling-Metrics-Domain";
    public static final String DEFAULT_DOMAIN_NAME = "org.apache.sling";
    static final String JMX_TYPE_METRICS = "Metrics";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<String, Bundle> metricToBundleMapping = new ConcurrentHashMap<>();
    private final MetricRegistry registry;

    BundleMetricsMapper(MetricRegistry registry) {
        this.registry = registry;
    }

    public void addMapping(String name, Bundle bundle) {
        metricToBundleMapping.putIfAbsent(name, bundle);
    }

    public void unregister(Set<String> registeredNames) {
        for (String name : registeredNames){
            registry.remove(name);
            metricToBundleMapping.remove(name);
        }
        log.debug("Removed metrics for {}", registeredNames);
    }

    @Override
    public ObjectName createName(String type, String domain, String name) {
        String mappedDomainName = JmxUtil.safeDomainName(getDomainName(name));
        if (mappedDomainName == null) {
            mappedDomainName = domain;
        }

        Hashtable<String, String> table = new Hashtable<>();
        table.put("type", JMX_TYPE_METRICS);
        table.put("name", JmxUtil.quoteValueIfRequired(name));
        try {
            return new ObjectName(mappedDomainName, table);
        } catch (MalformedObjectNameException e) {
            log.warn("Unable to register {} {}", type, name, e);
            throw new RuntimeException(e);
        }
    }

    private String getDomainName(String name) {
        Bundle bundle = metricToBundleMapping.get(name);
        return getDomainName(bundle);
    }

    private String getDomainName(Bundle bundle) {
        if (bundle == null){
            return null;
        }

        String domainNameHeader = bundle.getHeaders().get(HEADER_DOMAIN_NAME);
        if (domainNameHeader != null){
            return domainNameHeader;
        }

        //Fallback to symbolic name
        return bundle.getSymbolicName();
    }

}
