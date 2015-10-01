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

import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactoryPreconditions {

    private static final class RequiredProvider {
        public String pid;
        public Filter filter;
    };

    private ResourceProviderTracker tracker;

    private volatile List<RequiredProvider> requiredProviders;

    public void activate(final BundleContext bc, final String[] configuration, ResourceProviderTracker tracker) {
        this.tracker = tracker;

        final List<RequiredProvider> rps = new ArrayList<RequiredProvider>();
        if ( configuration != null ) {
            final Logger logger = LoggerFactory.getLogger(getClass());
            for(final String r : configuration) {
                if ( r != null && r.trim().length() > 0 ) {
                    final String value = r.trim();
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
        }
        this.requiredProviders = rps;
    }

    public void deactivate() {
        this.requiredProviders = null;
    }

    public boolean checkPreconditions() {
        synchronized ( this ) {
            boolean canRegister = false;
            if (this.requiredProviders != null) {
                canRegister = false;
                for (ResourceProviderHandler h : this.tracker.getResourceProviderStorage().getAllHandlers()) {
                    for (final RequiredProvider rp : this.requiredProviders) {
                        ServiceReference ref = h.getInfo().getServiceReference();
                        if (rp.filter != null && rp.filter.match(ref)) {
                            canRegister = true;
                            break;
                        } else if (rp.pid != null && rp.pid.equals(ref.getProperty(Constants.SERVICE_PID))){
                            canRegister = true;
                            break;
                        }
                    }
                }
            }
            return canRegister;
        }
    }
}