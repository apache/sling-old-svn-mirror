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

package org.apache.sling.commons.log.logback.integration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.osgi.framework.BundleContext;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.keepCaches;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.util.PathUtils.getBaseDir;

public abstract class LogTestBase {
    @Inject
    protected BundleContext bundleContext;

    // the name of the system property providing the bundle file to be installed
    // and tested
    protected static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    // the name of the system property which captures the jococo coverage agent
    // command
    // if specified then agent would be specified otherwise ignored
    protected static final String COVERAGE_COMMAND = "coverage.command";

    // the default bundle jar file name
    protected static final String BUNDLE_JAR_DEFAULT = "target/slinglogback.jar";

    // the JVM option to set to enable remote debugging
    @SuppressWarnings("UnusedDeclaration")
    protected static final String DEBUG_VM_OPTION = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=31313";

    // the actual JVM option set, extensions may implement a static
    // initializer overwriting this value to have the configuration()
    // method include it when starting the OSGi framework JVM
    protected static String paxRunnerVmOption = null;

    // Name of the property for port of server
    public static final String HTTP_PORT_PROP = "http.port";

    protected static String DEFAULT_PORT = "8080";

    @Configuration
    public Option[] config() throws IOException {
        final String bundleFileName = System.getProperty(BUNDLE_JAR_SYS_PROP, BUNDLE_JAR_DEFAULT);
        final File bundleFile = new File(bundleFileName);
        if (!bundleFile.canRead()) {
            throw new IllegalArgumentException("Cannot read from bundle file " + bundleFileName + " specified in the "
                + BUNDLE_JAR_SYS_PROP + " system property. Try building the project first " +
                    "with 'mvn clean install -Pide -DskipTests'");
        }
        return options(
            // the current project (the bundle under test)
            CoreOptions.bundle(bundleFile.toURI().toString()),
            mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(), addPaxExamSpecificOptions(),
            addCodeCoverageOption(), addDebugOptions(), addExtraOptions(), addDefaultOptions());
    }

    protected Option addPaxExamSpecificOptions() {
        return composite(junitBundles(), systemProperty("pax.exam.osgi.unresolved.fail").value("fail"));
    }

    protected Option addDefaultOptions() {
        return addSlingHome();
    }

    private static Option addDebugOptions() throws IOException {
        if (paxRunnerVmOption != null) {
            String workDir = FilenameUtils.concat(getBaseDir(), "target/pax");
            File workDirFile = new File(workDir);
            if (workDirFile.exists()) {
                FileUtils.deleteDirectory(workDirFile);
            }
            return composite(CoreOptions.vmOption(paxRunnerVmOption), keepCaches(),
                systemTimeout(TimeUnit.MINUTES.toMillis(10)), workingDirectory(workDir));
        }
        return null;
    }

    private static Option addCodeCoverageOption() {
        String coverageCommand = System.getProperty(COVERAGE_COMMAND);
        if (coverageCommand != null) {
            return CoreOptions.vmOption(coverageCommand);
        }
        return null;
    }

    private static Option addSlingHome() {
        String workDirName = FilenameUtils.concat(getBaseDir(), "target/sling");
        return systemProperty("sling.home").value(workDirName);
    }

    protected static Option configAdmin() {
        return mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject();
    }

    protected static Option webSupport() {
        return composite(mavenBundle("org.apache.felix", "org.apache.felix.http.bundle").versionAsInProject(),
            systemProperty("org.osgi.service.http.port").value(getServerPort()));
    }

    protected Option addExtraOptions() {
        return new DefaultCompositeOption();
    }

    protected static String absolutePath(String configFileName){
        return FilenameUtils.concat(new File(".").getAbsolutePath(), "src/test/resources/"+configFileName);
    }

    protected static void delay() {
        try {
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (InterruptedException ie) {
            // dont care
        }
    }

    protected static String getServerPort() {
        return System.getProperty(HTTP_PORT_PROP, DEFAULT_PORT);
    }
}
