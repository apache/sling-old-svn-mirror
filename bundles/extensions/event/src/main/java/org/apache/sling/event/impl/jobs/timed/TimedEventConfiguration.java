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
package org.apache.sling.event.impl.jobs.timed;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.impl.EnvironmentComponent;


/**
 * An event handler for timed events.
 *
 */
@Component(immediate=true)
@Service(value={TimedEventConfiguration.class})
@Property(name=TimedEventConfiguration.CONFIG_PROPERTY_REPOSITORY_PATH,
          value=TimedEventConfiguration.DEFAULT_REPOSITORY_PATH)
public class TimedEventConfiguration {

    /** Default repository path. */
    static final String DEFAULT_REPOSITORY_PATH = "/var/eventing/timed-jobs";

    /** The path where all jobs are stored. */
    static final String CONFIG_PROPERTY_REPOSITORY_PATH = "repository.path";

    @Reference
    private EnvironmentComponent environment;

    /** The path in the resource tree. */
    private String resourcePath;

    /** The path in the resource tree. */
    private String resourcePathWithSlash;

    /**
     * Activate this component.
     */
    @Activate
    protected void activate(final Map<String, Object> props) {
        this.resourcePath = PropertiesUtil.toString(props.get(CONFIG_PROPERTY_REPOSITORY_PATH),
                                   DEFAULT_REPOSITORY_PATH);
        this.resourcePathWithSlash = this.resourcePath.concat("/");
    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public String getResourcePathWithSlash() {
        return this.resourcePathWithSlash;
    }
}
