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
package org.apache.sling.event.impl.dea;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.support.Environment;

/**
 * This service wraps the configuration of the distributed event admin.
 */
@Component(name="org.apache.sling.event.impl.DistributingEventHandler")
@Service(value=DistributedEventAdminConfiguration.class)
public class DistributedEventAdminConfiguration {

    /** Default repository path. */
    private static final String DEFAULT_REPOSITORY_PATH = "/var/eventing/distribution";

    /** The path where all jobs are stored. */
    @Property(value=DEFAULT_REPOSITORY_PATH)
    private static final String CONFIG_PROPERTY_REPOSITORY_PATH = "repository.path";

    /** Default clean up time is 15 minutes. */
    private static final int DEFAULT_CLEANUP_PERIOD = 15;

    @Property(intValue=DEFAULT_CLEANUP_PERIOD)
    private static final String CONFIG_PROPERTY_CLEANUP_PERIOD = "cleanup.period";

    /** We remove everything which is older than 15min by default. */
    private int cleanupPeriod = DEFAULT_CLEANUP_PERIOD;

    /** The path in the resource tree. */
    private String rootPath;

    private String rootPathWithSlash;

    private String ownRootPath;

    private String ownRootPathSlash;

    @Reference
    private EnvironmentComponent environment;

    @Activate
    protected void activate(final Map<String, Object> props) {
        this.cleanupPeriod = PropertiesUtil.toInteger(props.get(CONFIG_PROPERTY_CLEANUP_PERIOD), DEFAULT_CLEANUP_PERIOD);
        this.rootPath = PropertiesUtil.toString(props.get(
                CONFIG_PROPERTY_REPOSITORY_PATH), DEFAULT_REPOSITORY_PATH);
        this.rootPathWithSlash = this.rootPath.concat("/");
        this.ownRootPath = this.rootPathWithSlash.concat(Environment.APPLICATION_ID);
        this.ownRootPathSlash = this.ownRootPath.concat("/");
    }

    /**
     * This is the root path for all events.
     * @return The path ending with a slash.
     */
    public String getRootPathWithSlash() {
        return this.rootPathWithSlash;
    }

    /**
     * This is the root path for all events.
     * @return The path ending with a slash.
     */
    public String getRootPath() {
        return this.rootPath;
    }

    /**
     * This is the root path for all events.
     * @return The path does not end with a slash.
     */
    public String getOwnRootPath() {
        return this.ownRootPath;
    }

    /**
     * This is the root path for all events of this instance.
     * @return The path ending with a slash.
     */
    public String getOwnRootPathWithSlash() {
        return this.ownRootPathSlash;
    }

    public int getCleanupPeriod() {
        return this.cleanupPeriod;
    }
}
