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
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.base.impl.Sling;
import org.osgi.framework.BundleException;

/**
 * Run a Launchpad application.
 */
@Mojo( name = "run", requiresDependencyResolution = ResolutionScope.TEST)
public class RunMojo extends AbstractLaunchpadStartingMojo {



    private Thread shutdown = new Thread() {
        /**
         * Called when the Java VM is being terminiated, for example because the
         * KILL signal has been sent to the process. This method calls stop on
         * the launched Sling instance to terminate the framework before
         * returning.
         */
        @Override
        public void run() {
            getLog().info("Java VM is shutting down", null);
            shutdown();
        }

        // ---------- Shutdown support for control listener and shutdown hook

        void shutdown() {
            // remove the shutdown hook, will fail if called from the
            // shutdown hook itself. Otherwise this prevents shutdown
            // from being called again
            try {
                Runtime.getRuntime().removeShutdownHook(this);
            } catch (Throwable t) {
                // don't care for problems removing the hook
            }

            stopSling();
        }
    };
    
    private boolean registeredHook = false;

    protected Sling startSling(LaunchpadContentProvider resourceProvider, final Map<String, String> props, Logger logger)
            throws BundleException {
        if (!registeredHook) {
            Runtime.getRuntime().addShutdownHook(shutdown);
            registeredHook = true;
        }
        
        // creating the instance launches the framework and we are done here
        Sling mySling = new Sling(this, logger, resourceProvider, props) {

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

        // TODO this seems hacky!
        try {
            while (mySling != null) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
        }

        return mySling;
    }


}
