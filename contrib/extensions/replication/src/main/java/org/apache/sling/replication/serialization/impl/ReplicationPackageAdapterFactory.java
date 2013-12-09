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
package org.apache.sling.replication.serialization.impl;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuilderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AdapterFactory} for
 * {@link org.apache.sling.replication.serialization.ReplicationPackage}s
 */
@Component
@Service(value = org.apache.sling.api.adapter.AdapterFactory.class)
@Properties({
        @Property(name = "adaptables", value = { "org.apache.sling.api.SlingHttpServletRequest" }),
        @Property(name = "adapters", value = { "org.apache.sling.replication.serialization.ReplicationPackage" }) })
@Deprecated
public class ReplicationPackageAdapterFactory implements AdapterFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationPackageBuilderProvider packageBuilderProvider;

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (log.isInfoEnabled()) {
            log.info("adapting package (from {} to {})", new Object[] { adaptable, type });
        }
        ReplicationPackage pkg = null;
        try {
            if (adaptable instanceof SlingHttpServletRequest) {
                SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;
                String name = request.getHeader("Type");
                ReplicationPackageBuilder replicationPackageBuilder = packageBuilderProvider
                                .getReplicationPackageBuilder(name);
                if (log.isInfoEnabled()) {
                    log.info("using {} package builder", replicationPackageBuilder);
                }
                if (replicationPackageBuilder != null) {
                    InputStream stream = request.getInputStream();
                    pkg = replicationPackageBuilder.readPackage(stream, true);
                    if (pkg != null) {
                        if (log.isInfoEnabled()) {
                            log.info("package {} created", Arrays.toString(pkg.getPaths()));
                        }
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn("could not read a package");
                        }
                    }
                } else {
                    if (log.isErrorEnabled()) {
                        log.error("could not read packages of type {}", type);
                    }
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error while adapting stream to a replication package", e);
            }
        }
        return (AdapterType) pkg;
    }
}
