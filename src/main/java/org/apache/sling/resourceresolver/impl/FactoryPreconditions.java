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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactoryPreconditions {

    private static final class RequiredProvider {
        public String pid;
        public Filter filter;
        public final List<Map<String, Object>> matchingServices = new ArrayList<Map<String,Object>>();
    };

    private final List<Map<String, Object>> earlyPropertiesList = new ArrayList<Map<String, Object>>();

    private volatile List<RequiredProvider> requiredProviders;

    private Boolean cachedResult = Boolean.FALSE;

    public void activate(final BundleContext bc, final String[] configuration) {
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
        synchronized ( this ) {
            this.requiredProviders = rps;
            this.cachedResult = null;
            for(final Map<String,Object> props : this.earlyPropertiesList) {
                this.bindProvider(props);
            }
            this.earlyPropertiesList.clear();
        }
    }

    public void deactivate() {
        this.requiredProviders = null;
    }

    public boolean checkPreconditions() {
        synchronized ( this ) {
            if ( cachedResult != null ) {
                return cachedResult;
            }
            boolean canRegister = false;
            if ( this.requiredProviders != null) {
                canRegister = true;
                for(final RequiredProvider rp : this.requiredProviders) {
                    if ( rp.matchingServices.size() == 0 ) {
                        canRegister = false;
                        break;
                    }
                }
            }
            this.cachedResult = canRegister;
            return canRegister;
        }
    }

    public void bindProvider(final Map<String, Object> props) {
        synchronized ( this ) {
            if ( this.requiredProviders == null ) {
                this.earlyPropertiesList.add(props);
                return;
            }
            Dictionary<String, Object> dict = null;
            for(final RequiredProvider rp : this.requiredProviders) {
                if ( rp.pid != null ) {
                    if ( rp.pid.equals(props.get(Constants.SERVICE_PID)) ) {
                        rp.matchingServices.add(props);
                        this.cachedResult = null;
                    }
                } else {
                    if ( dict == null ) {
                        dict = new Hashtable<String, Object>(props);
                    }
                    if ( rp.filter.match(dict) ) {
                        rp.matchingServices.add(props);
                        this.cachedResult = null;
                    }
                }
            }
        }
    }

    private boolean removeFromList(final List<Map<String, Object>> list, final Map<String, Object> props) {
        final Long id = (Long) props.get(Constants.SERVICE_ID);
        int index = 0;
        while ( index < list.size() ) {
            final Long currentId = (Long ) list.get(index).get(Constants.SERVICE_ID);
            if ( currentId == id ) {
                list.remove(index);
                return true;
            }
            index++;
        }
        return false;
    }

    public void unbindProvider(final Map<String, Object> props) {
        synchronized ( this ) {
            if ( this.requiredProviders == null ) {
                this.removeFromList(this.earlyPropertiesList, props);
                return;
            }
            for(final RequiredProvider rp : this.requiredProviders) {
                if ( this.removeFromList(rp.matchingServices, props) ) {
                    this.cachedResult = null;
                }
            }
        }
    }
}