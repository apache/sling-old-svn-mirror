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

}
