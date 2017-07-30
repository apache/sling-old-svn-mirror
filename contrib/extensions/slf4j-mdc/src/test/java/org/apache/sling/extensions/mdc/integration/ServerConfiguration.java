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

package org.apache.sling.extensions.mdc.integration;

import org.apache.sling.extensions.mdc.integration.servlet.MDCStateServlet;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.Constants;

import java.io.File;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

public class ServerConfiguration {

    // the name of the system property providing the bundle file to be installed and tested
    protected static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    // the name of the system property which captures the jococo coverage agent command
    //if specified then agent would be specified otherwise ignored
    protected static final String COVERAGE_COMMAND = "coverage.command";

    //Name of the property for port of server
    public static final String HTTP_PORT_PROP = "http.port";

    public static final String HTTP_SERVER__URL_PROP = "serverUrl";

    // the default bundle jar file name
    protected static final String BUNDLE_JAR_DEFAULT = "BUNDLE_JAR_NOT_SPECIFIED";

    // the JVM option to set to enable remote debugging
    @SuppressWarnings("UnusedDeclaration")
    protected static final String DEBUG_VM_OPTION = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=31313";

    // the actual JVM option set, extensions may implement a static
    // initializer overwriting this value to have the configuration()
    // method include it when starting the OSGi framework JVM
    protected static String paxRunnerVmOption = null;
//    protected static String paxRunnerVmOption = DEBUG_VM_OPTION;

    protected static String DEFAULT_PORT = "8080";

    @Configuration
    public Option[] config() {
        final String bundleFileName = System.getProperty(BUNDLE_JAR_SYS_PROP,
                BUNDLE_JAR_DEFAULT);
        final File bundleFile = new File(bundleFileName);
        if (!bundleFile.canRead()) {
            throw new IllegalArgumentException("Cannot read from bundle file "
                    + bundleFileName + " specified in the " + BUNDLE_JAR_SYS_PROP
                    + " system property");
        }
        Option[] base = options(
                // the current project (the bundle under test)
                CoreOptions.bundle(bundleFile.toURI().toString()),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.7.0").startLevel(2),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.7.0").startLevel(2),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.http.jetty").versionAsInProject().startLevel(3),
                mavenBundle("org.apache.felix", "org.apache.felix.http.whiteboard").versionAsInProject().startLevel(5),
                mavenBundle("org.apache.felix", "org.apache.felix.scr").versionAsInProject(),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.johnzon").versionAsInProject(),
                systemProperty("pax.exam.osgi.unresolved.fail").value("fail"),
                systemProperty("org.osgi.service.http.port").value(getServerPort()),
                cleanCaches(),
                createTestBundle(),
                addCodeCoverageOption());
        final Option vmOption = (paxRunnerVmOption != null) ? CoreOptions.vmOption(paxRunnerVmOption)
                : null;
        return OptionUtils.combine(base, vmOption);
    }

    private Option createTestBundle() {
        TinyBundle bundle = bundle()
                .add(MDCStateServlet.class)
                .set(Constants.BUNDLE_SYMBOLICNAME,"org.apache.sling.extensions.slf4j.mdc.testbundle")
                .set(Constants.BUNDLE_ACTIVATOR , MDCStateServlet.class.getName());
        return provision(bundle.build(withBnd()));
    }

    private static String getServerPort() {
        return System.getProperty(HTTP_PORT_PROP, DEFAULT_PORT);
    }

    private Option addCodeCoverageOption() {
        String coverageCommand = System.getProperty(COVERAGE_COMMAND);
        if (coverageCommand != null) {
            return CoreOptions.vmOption(coverageCommand);
        }
        return null;
    }

    public static String getServerUrl() {
        String serverUrl = System.getProperty(HTTP_SERVER__URL_PROP);
        if (serverUrl != null) {
            return serverUrl;
        }

        return String.format("http://localhost:%s", getServerPort());
    }
}
