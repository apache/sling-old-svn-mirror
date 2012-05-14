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

    /** Alias factory pid or null. */
    protected String aliasPid;

    /** Configuration admin. */
    private final ConfigurationAdmin configAdmin;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    AbstractConfigTask(final TaskResourceGroup r, final ConfigurationAdmin configAdmin) {
        super(r);
        this.configAdmin = configAdmin;
        this.configPid = (String)getResource().getAttribute(Constants.SERVICE_PID);
        this.factoryPid = (String)getResource().getAttribute(ConfigurationAdmin.SERVICE_FACTORYPID);
        if ( r.getAlias() != null ) {
            this.aliasPid = r.getAlias().substring(this.factoryPid.length() + 1);
        } else {
            this.aliasPid = null;
        }
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

    protected String getCompositeAliasPid() {
        if ( this.aliasPid == null || this.factoryPid == null ) {
            return null;
        }
        final String alias = factoryPid + "." + this.aliasPid;
        final int pos = this.getResource().getEntityId().indexOf(':');
        if ( this.getResource().getEntityId().substring(pos + 1).equals(alias) ) {
            return null;
        }
        return alias;
    }

    protected Dictionary<String, Object> getDictionary() {
        return this.getResource().getDictionary();
    }

    protected Configuration getConfiguration(final ConfigurationAdmin ca,
                                             final boolean createIfNeeded)
    throws IOException, InvalidSyntaxException {
        return ConfigUtil.getConfiguration(ca, this.factoryPid, (this.factoryPid != null && this.aliasPid != null ? this.aliasPid : this.configPid), createIfNeeded);
    }
}
