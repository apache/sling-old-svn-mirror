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
package org.apache.sling.extensions.webconsolesecurityprovider.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the common base class for the two provider implementations.
 * It handles the configuration of the service.
 */
public abstract class AbstractWebConsoleSecurityProvider
    implements WebConsoleSecurityProvider, ManagedService {

    // name of the property providing list of authorized users
    private static final String PROP_USERS = "users";

    // default user being authorized
    public static final String PROP_GROUPS_DEFAULT_USER = "admin";

    // name of the property providing list of groups whose members are
    // authorized
    private static final String PROP_GROUPS = "groups";

    /** default logger */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Set<String> users = Collections.singleton(PROP_GROUPS_DEFAULT_USER);

    protected Set<String> groups = Collections.emptySet();

    /**
     * Handle configuration
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    public void updated(final Dictionary properties)
            throws ConfigurationException {
        this.users = toSet(properties == null ? null : properties.get(PROP_USERS), PROP_GROUPS_DEFAULT_USER);
        this.groups = toSet(properties == null ? null : properties.get(PROP_GROUPS), null);
    }

    private Set<String> toSet(final Object configObj, final String defaultUser) {
        final Set<String> groups = new HashSet<String>();
        if (configObj instanceof String) {
            groups.add((String) configObj);
        } else if (configObj instanceof Collection<?>) {
            for (Object obj : ((Collection<?>) configObj)) {
                if (obj instanceof String) {
                    groups.add((String) obj);
                }
            }
        } else if (configObj instanceof String[]) {
            for (String string : ((String[]) configObj)) {
                if (string != null) {
                    groups.add(string);
                }
            }
        } else if (configObj == null && defaultUser != null) {
            groups.add(defaultUser);
        }
        return groups;
    }
}
