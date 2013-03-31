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
package org.apache.sling.launchpad.karaf;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupMode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 * figure out when we are finished @see #isFinished()
 */
@Component(
    name = "org.apache.sling.launchpad.karaf.KarafStartupHandler",
    label = "%org.apache.sling.launchpad.karaf.label",
    description = "%org.apache.sling.launchpad.karaf.description",
    metatype = true,
    immediate = true
)
@Service
@Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")
public class KarafStartupHandler implements StartupHandler {

    private StartupMode startupMode;

    private static final String STARTUP_MODE_INSTALL = "INSTALL";

    private static final String STARTUP_MODE_RESTART = "RESTART";

    private static final String STARTUP_MODE_UPDATE = "UPDATE";

    /**
     * TODO
     * until fully figured out what happens on INSTALL we use RESTART even on initial startup as Sling is booting and
     * running fine on Karaf with RESTART
     */
    private static final String DEFAULT_STARTUP_MODE = STARTUP_MODE_RESTART;

    @Property(
        value = DEFAULT_STARTUP_MODE,
        options = {
            @PropertyOption(name = STARTUP_MODE_INSTALL, value = "install"),
            @PropertyOption(name = STARTUP_MODE_RESTART, value = "restart"),
            @PropertyOption(name = STARTUP_MODE_UPDATE, value = "update")
        })
    private static final String STARTUP_MODE_PARAMETER_NAME = "org.apache.sling.launchpad.startupmode";

    private final Logger logger = LoggerFactory.getLogger(KarafStartupHandler.class);

    public KarafStartupHandler() {
    }

    @Activate
    private void activate(final BundleContext bundleContext, final Map<String, Object> properties) {
        logger.info("activate('{}', '{}')", bundleContext, properties);
        configure(properties);
    }

    @Modified
    private void modified(Map<String, Object> properties) {
        logger.info("modified('{}')", properties);
        configure(properties);
    }

    @Deactivate
    private void deactivate(final BundleContext bundleContext) {
        logger.info("deactivate('{}')", bundleContext);
    }

    private void configure(Map<String, Object> properties) {
        logger.info("configure('{}')", properties);
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        // startup mode
        String startupMode = (String) properties.get(STARTUP_MODE_PARAMETER_NAME);
        if (startupMode == null) {
            startupMode = DEFAULT_STARTUP_MODE;
        }
        this.startupMode = StartupMode.valueOf(startupMode);
        logger.info("configured startup mode: {}", this.startupMode);
    }

    @Override
    public StartupMode getMode() {
        logger.info("getMode(): {}", startupMode);
        return startupMode;
    }

    // TODO
    @Override
    public boolean isFinished() {
        logger.info("isFinished()");
        return true;
    }

    // TODO
    @Override
    public void waitWithStartup(boolean flag) {
        logger.info("waitWithStartup({})", flag);
    }

}
