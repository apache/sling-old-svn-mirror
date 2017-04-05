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
package org.apache.sling.resourceresolver.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.sling.resourceresolver.impl.legacy.LegacyResourceProviderWhiteboard;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class which checks whether all conditions for registering
 * the resource resolver factory are fulfilled.
 */
public class FactoryPreconditions {

    private static final class RequiredProvider {
        public String name;
        public String pid;
        public Filter filter;
    };

    private volatile ResourceProviderTracker tracker;

    private volatile List<RequiredProvider> requiredProviders;

    public void activate(final BundleContext bc,
            final Set<String> legycyConfiguration,
            final Set<String> namesConfiguration,
            final ResourceProviderTracker tracker) {
        synchronized ( this ) {
            this.tracker = tracker;

            final List<RequiredProvider> rps = new ArrayList<RequiredProvider>();
            if ( legycyConfiguration != null ) {
                final Logger logger = LoggerFactory.getLogger(getClass());
                for(final String value : legycyConfiguration) {
                    RequiredProvider rp = new RequiredProvider();
                    if ( value.startsWith("(") ) {
                        try {
                            rp.filter = bc.createFilter(value);
                        } catch (final InvalidSyntaxException e) {
                            logger.warn("Ignoring invalid filter syntax for required provider: " + value, e);
                            rp = null;
                        }
                    } else {
                        rp.pid = value;
                    }
                    if ( rp != null ) {
                        rps.add(rp);
                    }
                }
            }
            if ( namesConfiguration != null ) {
                for(final String value : namesConfiguration) {
	                final RequiredProvider rp = new RequiredProvider();
	                rp.name = value;
	                rps.add(rp);
                }
            }
            this.requiredProviders = rps;
        }
    }

    public void deactivate() {
        synchronized ( this ) {
            this.requiredProviders = null;
            this.tracker = null;
        }
    }

    public boolean checkPreconditions(final String unavailableName, final String unavailableServicePid) {
        synchronized ( this ) {
            final List<RequiredProvider> localRequiredProviders = this.requiredProviders;
            final ResourceProviderTracker localTracker = this.tracker;
            boolean canRegister = localTracker != null;
            if (localRequiredProviders != null && localTracker != null ) {
                for (final RequiredProvider rp : localRequiredProviders) {
                    canRegister = false;
                    for (final ResourceProviderHandler h : localTracker.getResourceProviderStorage().getAllHandlers()) {
                        final ServiceReference ref = h.getInfo().getServiceReference();
                        final Object servicePid = ref.getProperty(Constants.SERVICE_PID);
                        if ( unavailableServicePid != null && unavailableServicePid.equals(servicePid) ) {
                            // ignore this service
                            continue;
                        }
                        if ( unavailableName != null && unavailableName.equals(h.getInfo().getName()) ) {
                            // ignore this service
                            continue;
                        }
                        if ( rp.name != null && rp.name.equals(h.getInfo().getName()) ) {
                            canRegister = true;
                            break;
                        } else if (rp.filter != null && rp.filter.match(ref)) {
                            canRegister = true;
                            break;
                        } else if (rp.pid != null && rp.pid.equals(servicePid)){
                            canRegister = true;
                            break;
                        } else if (rp.pid != null && rp.pid.equals(ref.getProperty(LegacyResourceProviderWhiteboard.ORIGINAL_SERVICE_PID))) {
                            canRegister = true;
                            break;
                        }
                    }
                    if ( !canRegister ) {
                        break;
                    }
                }
            }
            return canRegister;
        }
    }
}
