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
package org.apache.sling.settings;

import java.net.URL;
import java.util.Set;

/**
 * The <code>SlingSettingsService</code> provides basic Sling settings.
 * - Sling home : If the Sling launchpad is used
 * - Sling Id : A unique id of the installation
 *
 * Run Mode Support
 * A run mode is simply a string like "author", "test", "development",...
 * The server can have a set of active run modes.
 */
public interface SlingSettingsService {

    /**
     * The name of the framework property defining the Sling home directory
     * (value is "sling.home"). This is a Platform file system directory below
     * which all runtime data, such as the Felix bundle archives, logfiles, the
     * repository, etc., is located.
     * <p>
     * This property is available calling the
     * <code>BundleContext.getProperty(String)</code> method.
     *
     * @see #SLING_HOME_URL
     */
    String SLING_HOME = "sling.home";

    /**
     * The name of the framework property defining the Sling home directory as
     * an URL (value is "sling.home.url").
     * <p>
     * The value of this property is assigned the value of
     * <code>new File(${sling.home}).toURI().toString()</code> before
     * resolving the property variables.
     * <p>
     * This property is available calling the
     * <code>BundleContext.getProperty(String)</code> method.
     *
     * @see #SLING_HOME
     */
    String SLING_HOME_URL = "sling.home.url";

    /**
     * The name of the framework property defining the set of used
     * run modes.
     * The value is a comma separated list of run modes.
     */
    String RUN_MODES_PROPERTY = "sling.run.modes";

    /**
     * The name of the framework property defining the list
     * of run mode options
     * The value is a comma separated list of options where each option
     * contains of a set of run modes separated by a | character.
     * @since 1.2.0
     */
    String RUN_MODE_OPTIONS = "sling.run.mode.options";

    /**
     * The name of the framework property defining the list
     * of run mode options for installation time.
     * The value is a comma separated list of options where each option
     * contains of a set of run modes separated by a | character.
     * @since 1.2.0
     */
    String RUN_MODE_INSTALL_OPTIONS = "sling.run.mode.install.options";

    /**
     * Utility method to generate an absolute path
     * within Sling Home.
     *
     * @since 1.1.0
     */
    String getAbsolutePathWithinSlingHome(String relativePath);

    /**
     * The identifier of the running Sling instance.
     */
    String getSlingId();

    /**
     * Returns the value of the {@link #SLING_HOME}
     * property.
     */
    String getSlingHomePath();

    /**
     * Returns the value of the {@link #SLING_HOME_URL}
     * property.
     */
    URL getSlingHome();

    /**
     * Return the set of activate run modes.
     * This set might be empty.
     * @return A non modifiable set of run modes.
     */
    Set<String> getRunModes();

    /**
     * Return the optional name of the instance.
     * @return The name of the instance or <code>null</code>.
     */
    String getSlingName();

    /**
     * Return the optional description of the instance.
     * @return The description of the instance or <code>null</code>.
     */
    String getSlingDescription();
}
