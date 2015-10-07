/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.maven.projectsupport;

import java.util.Map;

import org.apache.felix.framework.Logger;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.base.impl.Sling;
import org.osgi.framework.BundleException;

/**
 * Start a Launchpad application.
 */
@Mojo( name = "start", requiresDependencyResolution = ResolutionScope.TEST)
public class StartMojo extends AbstractLaunchpadStartingMojo {

    @Parameter( property = "sling.control.port", defaultValue = "63000")
    private int controlPort;

    @Parameter(property = "sling.control.host")
    private String controlHost;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Sling startSling(LaunchpadContentProvider resourceProvider, final Map<String, String> props, Logger logger)
            throws BundleException {
        new ControlListener(this, getLog(), controlHost, controlPort).listen();

        return new Sling(this, logger, resourceProvider, props) {

            // overwrite the loadPropertiesOverride method to inject the
            // mojo arguments unconditionally. These will not be persisted
            // in any properties file, though
            protected void loadPropertiesOverride(
                    Map<String, String> properties) {
                if (props != null) {
                    properties.putAll(props);
                }
            }
        };
    }

}
