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
package org.apache.sling.launchpad.base.shared;

public interface SharedConstants {

    /**
     * The name of the configuration property defining the Sling home directory
     * (value is "sling.home"). This is a Platform file system directory below
     * which all runtime data, such as the Felix bundle archives, logfiles,
     * Jackrabbit repository, etc., is located.
     * <p>
     * The value of this property is derived in a launcher specific way, such as
     * system property, command line option, servlet or servlet context
     * init-param, servlet context path, etc.
     */
    public static final String SLING_HOME = "sling.home";

    /**
     * Default {@link #SLING_HOME sling.home} value if no other value can be
     * resolvled (value is "sling").
     */
    public static final String SLING_HOME_DEFAULT = "sling";

    /**
     * The fully qualified name of the class used by the Standalone Java
     * Application main class to control the framework. This class implements
     * the {@link Launcher} interface.
     */
    public static final String DEFAULT_SLING_MAIN = "org.apache.sling.launchpad.base.app.MainDelegate";

    /**
     * The fully qualified name of the class used by the Sling Web Applicaiton
     * servlet to control the framework. This class implements the
     * <code>javax.servlet.Servlet</code> and {@link Launcher} (for setup only)
     * interfaces.
     */
    public static final String DEFAULT_SLING_SERVLET = "org.apache.sling.launchpad.base.webapp.SlingServletDelegate";

    /**
     * The fully qualified name of the implementation of the Servlet API
     * ServletContextListener, HttpSessionListener, and
     * HttpSessionAttributeListener interfaces to which the respective events
     * are forwarded.
     */
    public static final String DEFAULT_SLING_LISTENER = "org.apache.sling.launchpad.base.webapp.SlingHttpSessionListenerDelegate";

    /**
     * The name of the file providing the Launcher JAR. On the one hand this is
     * the name used to place the JAR file sling.home to use for startup. On the
     * other hand, this is the name of the file in the archive (see
     * {@link #DEFAULT_SLING_LAUNCHER_JAR}).
     */
    public static final String LAUNCHER_JAR_REL_PATH = "org.apache.sling.launchpad.base.jar";

    /**
     * The absolute path to the launcher JAR file in the archive to copy to the
     * sling.home directory.
     */
    public static final String DEFAULT_SLING_LAUNCHER_JAR = "/resources/"
        + LAUNCHER_JAR_REL_PATH;

    /**
     * True or false value which controls whether sling will load bundles which
     * are contained in the resources/# path locations in the sling jar or war <br/>
     * The default is to unpack the jars and deploy them to the startup folder
     * in sling home
     */
    public static final String DISABLE_PACKAGE_BUNDLE_LOADING = "org.apache.sling.launchpad.disable.package.bundle.loading";

    /**
     * True or false value which controls whether sling will load bundles which
     * are contained in the resources/# path locations in the sling jar or war <br/>
     * regardless of the modification time of the Launchpad JAR.
     */
    public static final String FORCE_PACKAGE_BUNDLE_LOADING = "org.apache.sling.launchpad.force.package.bundle.loading";


    /**
     * The name of the configuration property defining the Sling properties file
     * (value is "sling.properties"). This is a Platform file system file
     * containing the startup configuration of Sling.
     * @since 2.2
     */
    public static final String SLING_PROPERTIES = "sling.properties";

    /**
     * The name of the configuration property defining the Sling properties url
     * (value is "sling.properties.url"). This is a url pointing to a resource
     * containing the startup configuration of Sling.
     * @since 2.2
     */
    public static final String SLING_PROPERTIES_URL = "sling.properties.url";

    /**
     * The name of the configuration property defining the location for the
     * Sling launchpad JAR file and the startup folder containing bundles
     * to be installed by the Bootstrap Installer (value is "sling.launchpad").
     * @since 2.4.0
     */
    public static final String SLING_LAUNCHPAD = "sling.launchpad";

    /**
     * The name of the configuration property defining if the startup level
     * is increased incrementally for installs and startups.
     * If enabled the framework starts with the start level defined by
     * {@link #SLING_INSTALL_STARTLEVEL}
     * and the startup manager increases the start level one by one until
     * the initial framework start level is reached (value is "sling.framework.install.incremental").
     * The default value is false, disabling this feature.
     * @since 2.4.0
     * @deprecated This property is not used anymore.
     */
    @Deprecated
    public static final String SLING_INSTALL_INCREMENTAL_START = "sling.framework.install.incremental";

    /**
     * The name of the configuration property defining the startlevel
     * for installs and updates. The framework starts with this start level
     * and the startup manager increases the start level one by one until
     * the initial framework start level is reached (value is "sling.framework.install.startlevel").
     * This level is only used if {@link #SLING_INSTALL_INCREMENTAL_START} is
     * enabled. Default value is 10.
     * @since 2.4.0
     * @deprecated This property is not used anymore.
     */
    @Deprecated
    public static final String SLING_INSTALL_STARTLEVEL = "sling.framework.install.startlevel";
}
