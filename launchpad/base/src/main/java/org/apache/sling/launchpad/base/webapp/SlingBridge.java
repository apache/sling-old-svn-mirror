/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad.base.webapp;

import java.util.Map;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.base.impl.ResourceProvider;
import org.apache.sling.launchpad.base.impl.Sling;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.eclipse.equinox.http.servlet.internal.Activator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;

/**
 * The <code>SlingBridge</code> extends the base <code>Sling</code> class
 * calling the Eclipse Equinox Http Service activator for the proxy servlet to
 * be able to handle requests.
 */
public class SlingBridge extends Sling {

    // The Equinox Http Service activator
    private BundleActivator httpServiceActivator;

    public SlingBridge(Notifiable notifiable, Logger logger,
            ResourceProvider resourceProvider, Map<String, String> propOverwrite)
            throws BundleException {
        super(notifiable, logger, resourceProvider, propOverwrite);
    }

    @Override
    protected void doStartBundle() throws Exception {
        // activate the HttpService
        this.httpServiceActivator = new Activator();
        this.httpServiceActivator.start(this.getBundleContext());
    }

    @Override
    protected void doStopBundle() {
        if (this.httpServiceActivator != null) {
            try {
                this.httpServiceActivator.stop(this.getBundleContext());
            } catch (Exception e) {
                logger.log(Logger.LOG_ERROR,
                    "Unexpected problem stopping HttpService", e);
            }
            this.httpServiceActivator = null;
        }
    }
}
