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
package org.apache.sling.installer.factories.configuration.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for configuration-related tasks
 */
abstract class AbstractConfigTask extends InstallTask {

    /** Configuration PID */
    protected final String configPid;

    /** Factory PID or null */
    protected final String factoryPid;

    /** Configuration admin. */
    private final ConfigurationAdmin configAdmin;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    AbstractConfigTask(final TaskResourceGroup r, final ConfigurationAdmin configAdmin) {
        super(r);
        this.configAdmin = configAdmin;
        this.configPid = (String)getResource().getAttribute(Constants.SERVICE_PID);
        this.factoryPid = (String)getResource().getAttribute(ConfigurationAdmin.SERVICE_FACTORYPID);
    }

    protected Logger getLogger() {
        return this.logger;
    }

    /**
     * Get the configuration admin - if available
     */
    protected ConfigurationAdmin getConfigurationAdmin() {
        return this.configAdmin;
    }

    protected String getCompositePid() {
        return (factoryPid == null ? "" : factoryPid + ".") + configPid;
    }

    protected Dictionary<String, Object> getDictionary() {
        // Copy dictionary and add pseudo-properties
        final Dictionary<String, Object> d = this.getResource().getDictionary();
        if ( d == null ) {
            return null;
        }

        final Dictionary<String, Object> result = new Hashtable<String, Object>();
        final Enumeration<String> e = d.keys();
        while(e.hasMoreElements()) {
            final String key = e.nextElement();
            result.put(key, d.get(key));
        }

        result.put(ConfigTaskCreator.CONFIG_PATH_KEY, getResource().getURL());
        if ( this.factoryPid != null ) {
            result.put(ConfigTaskCreator.ALIAS_KEY, configPid);
        }

        return result;
    }

    protected Configuration getConfiguration(final ConfigurationAdmin ca,
                                             final boolean createIfNeeded)
    throws IOException, InvalidSyntaxException {
        return ConfigUtil.getConfiguration(ca, this.factoryPid, this.configPid, createIfNeeded, true);
    }
}
