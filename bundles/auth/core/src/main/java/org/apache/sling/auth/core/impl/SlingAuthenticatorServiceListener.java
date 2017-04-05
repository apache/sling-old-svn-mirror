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
package org.apache.sling.auth.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

public class SlingAuthenticatorServiceListener implements AllServiceListener {

    private final PathBasedHolderCache<AuthenticationRequirementHolder> authRequiredCache;

    private final HashMap<Object, Set<String>> regProps = new HashMap<Object, Set<String>>();

    private final HashMap<Object, List<AuthenticationRequirementHolder>> props = new HashMap<Object, List<AuthenticationRequirementHolder>>();

    static SlingAuthenticatorServiceListener createListener(
        final BundleContext context,
        final PathBasedHolderCache<AuthenticationRequirementHolder> authRequiredCache) {
        SlingAuthenticatorServiceListener listener = new SlingAuthenticatorServiceListener(authRequiredCache);
        try {
            final String filter = "(" + AuthConstants.AUTH_REQUIREMENTS + "=*)";
            context.addServiceListener(listener, filter);
            ServiceReference<?>[] refs = context.getAllServiceReferences(null,
                filter);
            if (refs != null) {
                for (ServiceReference<?> ref : refs) {
                    listener.addService(ref);
                }
            }
            return listener;
        } catch (InvalidSyntaxException ise) {
        }
        return null;
    }

    private SlingAuthenticatorServiceListener(final PathBasedHolderCache<AuthenticationRequirementHolder> authRequiredCache) {
        this.authRequiredCache = authRequiredCache;
    }

    @Override
    public void serviceChanged(final ServiceEvent event) {
        synchronized ( props ) {
            // modification of service properties, unregistration of the
            // service or service properties does not contain requirements
            // property any longer (new event with type 8 added in OSGi Core
            // 4.2)
            if ((event.getType() & (ServiceEvent.UNREGISTERING | ServiceEvent.MODIFIED_ENDMATCH)) != 0) {
                removeService(event.getServiceReference());
            }

            if ((event.getType() & ServiceEvent.MODIFIED ) != 0) {
                modifiedService(event.getServiceReference());
            }

            // add requirements for newly registered services and for
            // updated services
            if ((event.getType() & (ServiceEvent.REGISTERED | ServiceEvent.MODIFIED)) != 0) {
                addService(event.getServiceReference());
            }
        }
    }

    /**
     * Register all known services.
     */
    void registerAllServices() {
        for(final List<AuthenticationRequirementHolder> authReqs : props.values()) {
            registerService(authReqs);
        }
    }

    /**
     * Register all authentication requirement holders.
     * @param authReqs The auth requirement holders
     */
    private void registerService(final List<AuthenticationRequirementHolder> authReqs) {
        for (AuthenticationRequirementHolder authReq : authReqs) {
            authRequiredCache.addHolder(authReq);
        }
    }

    private Set<String> buildPathsSet(final String[] authReqPaths) {
        final Set<String> paths = new HashSet<>();
        for(final String authReq : authReqPaths) {
            if (authReq != null && authReq.length() > 0) {
                paths.add(authReq);
            }
        }
        return paths;
    }

    /**
     * Process a new service with auth requirements
     * @param ref The service reference
     */
    private void addService(final ServiceReference<?> ref) {
        final String[] authReqPaths = PropertiesUtil.toStringArray(ref.getProperty(AuthConstants.AUTH_REQUIREMENTS));
        if ( authReqPaths != null ) {
            final Set<String> paths = buildPathsSet(authReqPaths);

            if ( !paths.isEmpty() ) {
                final List<AuthenticationRequirementHolder> authReqList = new ArrayList<AuthenticationRequirementHolder>();
                for (final String authReq : paths) {
                    authReqList.add(AuthenticationRequirementHolder.fromConfig(
                        authReq, ref));
                }

                // keep original set for modified
                regProps.put(ref.getProperty(Constants.SERVICE_ID), paths);
                registerService(authReqList);
                props.put(ref.getProperty(Constants.SERVICE_ID), authReqList);
            }
        }
    }

    /**
     * Process a modified service with auth requirements
     * @param ref The service reference
     */
    private void modifiedService(final ServiceReference<?> ref) {
        final String[] authReqPaths = PropertiesUtil.toStringArray(ref.getProperty(AuthConstants.AUTH_REQUIREMENTS));
        if ( authReqPaths != null ) {
            final Set<String> oldPaths = regProps.get(ref.getProperty(Constants.SERVICE_ID));
            if ( oldPaths == null ) {
                addService(ref);
            } else {
                final Set<String> paths = buildPathsSet(authReqPaths);
                if ( paths.isEmpty() ) {
                    removeService(ref);
                } else {
                    final List<AuthenticationRequirementHolder> authReqs = props.get(ref.getProperty(Constants.SERVICE_ID));
                    // compare sets
                    for(final String oldPath : oldPaths) {
                        if ( !paths.contains(oldPath) ) {
                            // remove
                            final AuthenticationRequirementHolder holder = AuthenticationRequirementHolder.fromConfig(oldPath, ref);
                            authReqs.remove(holder);
                            authRequiredCache.removeHolder(holder);
                        }
                    }
                    for(final String path : paths) {
                        if ( !oldPaths.contains(path) ) {
                            // add
                            final AuthenticationRequirementHolder holder = AuthenticationRequirementHolder.fromConfig(path, ref);
                            authReqs.add(holder);
                            authRequiredCache.addHolder(holder);
                        }
                    }
                    regProps.put(ref.getProperty(Constants.SERVICE_ID), paths);
                }
            }
        } else {
            removeService(ref);
        }
    }

    /**
     * Process a removed service with auth requirements
     * @param ref The service reference
     */
    private void removeService(final ServiceReference<?> ref) {
        final List<AuthenticationRequirementHolder> authReqs = props.remove(ref.getProperty(Constants.SERVICE_ID));
        if (authReqs != null) {
            for (final AuthenticationRequirementHolder authReq : authReqs) {
                authRequiredCache.removeHolder(authReq);
            }
        }
        regProps.remove(ref.getProperty(Constants.SERVICE_ID));
    }
}
