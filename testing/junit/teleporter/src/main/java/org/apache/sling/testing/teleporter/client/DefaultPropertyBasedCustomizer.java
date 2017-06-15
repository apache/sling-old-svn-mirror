/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.teleporter.client;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.junit.rules.TeleporterRule.Customizer;

/**
 * This is the default {@link Customizer} which is used in case {@code TeleporterRule.forClass(Class)} was called. It relies on system
 * properties to configure the important aspects of teleporting. <br>
 * It assumes that there is already a running Sling server at the given baseUrl. To provision such a server the
 * <a href="https://sling.apache.org/documentation/development/slingstart.html#slingstart-maven-plugin">slingstart-maven-plugin</a> could be
 * used for example. <br>
 * The following system properties must be set for this Customizer to work
 * <ul>
 * <li>{@code ClientSideTeleporter.baseUrl}, the server url on which Sling is already running</li>
 * </ul>
 * 
 * The following system properties may be set to further tweak the behavior:
 * <ul>
 * <li>{@code ClientSideTeleporter.includeDependencyPrefixes}, comma-separated list of package prefixes for classes referenced from the IT.
 * Only the classes having one of the given package prefix are included in the bundle being deployed to the given Sling instance together
 * with the IT class itself. They are only included though in case they are referenced! If this is not set, no referenced classes will be
 * included.</li>
 * <li>{@code ClientSideTeleporter.excludeDependencyPrefixes}, comma-separated list of package prefixes for classes referenced from the IT.
 * Classes having one of the given package prefix will not be included in the bundle being deployed to the given Sling instance together
 * with the IT class itself. This takes precedence over the {@code ClientSideTeleporter.includeDependencyPrefixes}.</li>
 * <li>{@code ClientSideTeleporter.embedClasses}, comma-separated list of fully qualified class names which should be embedded in the test
 * bundle. Use this only for classes which are not detected automatically by the Maven Dependency Analyzer but still should be embedded in
 * the test bundle</li>
 * <li>{@code ClientSideTeleporter.embedClassesDirectories}, comma-separated list directories containing class files which should be
 * embedded in the test bundle. Use this only for classes which are not detected automatically by the Maven Dependency Analyzer but still
 * should be embedded in the test bundle</li>
 * <li>{@code ClientSideTeleporter.additionalBundleHeaders}, comma-separated list of entries in the format {@code <name>:<value>} which
 * should be added to the test bundle as additional headers</li>
 * <li>{@code ClientSideTeleporter.testReadyTimeoutSeconds}, how long to wait for our test to be ready on the server-side in seconds, after
 * installing the test bundle. By default {@code 12}.</li>
 * <li>{@code ClientSideTeleporter.serverUsername}, the username with which to send requests to the Sling server. By default
 * {@code admin}.</li>
 * <li>{@code ClientSideTeleporter.serverPassword}, the password with which to send requests to the Sling server. By default
 * {@code admin}.</li>
 * <li>{@code ClientSideTeleporter.enableLogging}, set to true to log the tasks being performed by the teleporter. Useful for
 * debugging.</li>
 * <li>{@code ClientSideTeleporter.preventToUninstallBundle}, set to true to not automatically uninstall the test bundle after test
 * execution. Useful for debugging.</li>
 * <li>{@code ClientSideTeleporter.testBundleDirectory}, if set the test bundles are being persisted (before being installed) within the
 * given directory name. If the directory does not exist, it will be automatically created. Useful for debugging.</li>
 * </ul>
*/
public class DefaultPropertyBasedCustomizer implements Customizer {
    static final String PROPERTY_BASE_URL = "ClientSideTeleporter.baseUrl";
    static final String PROPERTY_INCLUDE_DEPENDENCY_PREFIXES = "ClientSideTeleporter.includeDependencyPrefixes";
    static final String PROPERTY_EXCLUDE_DEPENDENCY_PREFIXES = "ClientSideTeleporter.excludeDependencyPrefixes";
    static final String PROPERTY_EMBED_CLASSES = "ClientSideTeleporter.embedClasses";
    static final String PROPERTY_EMBED_CLASSES_DIRECTORIES = "ClientSideTeleporter.embedClassesDirectories";
    static final String PROPERTY_SERVER_PASSWORD = "ClientSideTeleporter.serverPassword";
    static final String PROPERTY_SERVER_USERNAME = "ClientSideTeleporter.serverUsername";
    static final String PROPERTY_TESTREADY_TIMEOUT_SECONDS = "ClientSideTeleporter.testReadyTimeoutSeconds";
    static final String PROPERTY_TESTBUNDLE_DIRECTORY = "ClientSideTeleporter.testBundleDirectory";
    static final String PROPERTY_ENABLE_LOGGING = "ClientSideTeleporter.enableLogging";
    static final String PROPERTY_PREVENT_TO_UNINSTALL_BUNDLE = "ClientSideTeleporter.preventToUninstallBundle";
    static final String PROPERTY_ADDITIONAL_BUNDLE_HEADERS = "ClientSideTeleporter.additionalBundleHeaders";

    static final String LIST_SEPARATOR = ",";
    static final String NAME_VALUE_SEPARATOR = ":";

    private final int testReadyTimeout;
    private final String serverUsername;
    private final String serverPassword;
    private final String includeDependencyPrefixes;
    private final String excludeDependencyPrefixes;
    private final String embedClasses;
    private final String embedClassesDirectories;
    private final String baseUrl;
    private final String testBundleDirectory;
    private final boolean enableLogging;
    private final boolean preventToUninstallBundle;
    private final String additionalBundleHeaders;

    public DefaultPropertyBasedCustomizer() {
        testReadyTimeout = Integer.getInteger(PROPERTY_TESTREADY_TIMEOUT_SECONDS, 12);
        serverUsername = System.getProperty(PROPERTY_SERVER_USERNAME, "admin");
        serverPassword = System.getProperty(PROPERTY_SERVER_PASSWORD, "admin");
        includeDependencyPrefixes = System.getProperty(PROPERTY_INCLUDE_DEPENDENCY_PREFIXES);
        excludeDependencyPrefixes = System.getProperty(PROPERTY_EXCLUDE_DEPENDENCY_PREFIXES);
        embedClasses = System.getProperty(PROPERTY_EMBED_CLASSES);
        embedClassesDirectories = System.getProperty(PROPERTY_EMBED_CLASSES_DIRECTORIES);
        baseUrl = System.getProperty(PROPERTY_BASE_URL);
        additionalBundleHeaders = System.getProperty(PROPERTY_ADDITIONAL_BUNDLE_HEADERS);
        testBundleDirectory = System.getProperty(PROPERTY_TESTBUNDLE_DIRECTORY);
        enableLogging = Boolean.getBoolean(PROPERTY_ENABLE_LOGGING);
        preventToUninstallBundle = Boolean.getBoolean(PROPERTY_PREVENT_TO_UNINSTALL_BUNDLE);
    }

    @Override
    public void customize(TeleporterRule rule, String options) {
        final ClientSideTeleporter cst = (ClientSideTeleporter)rule;
        if (StringUtils.isBlank(baseUrl)) {
            fail("The mandatory system property " + PROPERTY_BASE_URL + " is not set!");
        }
        cst.setEnableLogging(enableLogging);
        cst.setPreventToUninstallBundle(preventToUninstallBundle);
        if (StringUtils.isNotBlank(testBundleDirectory)) {
            cst.setDirectoryForPersistingTestBundles(new File(testBundleDirectory));
        }
        cst.setBaseUrl(baseUrl);
        cst.setServerCredentials(serverUsername, serverPassword);
        cst.setTestReadyTimeoutSeconds(testReadyTimeout);
        if (StringUtils.isNotBlank(includeDependencyPrefixes)) {
            for (String includeDependencyPrefix : includeDependencyPrefixes.split(LIST_SEPARATOR)) {
                if (StringUtils.isNotBlank(includeDependencyPrefix)) {
                    cst.includeDependencyPrefix(includeDependencyPrefix);
                }
            }
        }
        if (StringUtils.isNotBlank(excludeDependencyPrefixes)) {
            for (String excludeDependencyPrefix : excludeDependencyPrefixes.split(LIST_SEPARATOR)) {
                if (StringUtils.isNotBlank(excludeDependencyPrefix)) {
                    cst.excludeDependencyPrefix(excludeDependencyPrefix);
                }
            }
        }
        if (StringUtils.isNotBlank(embedClassesDirectories)) {
            for (String embedClassesDirectory : embedClassesDirectories.split(LIST_SEPARATOR)) {
                if (StringUtils.isNotBlank(embedClassesDirectory)) {
                    try {
                        cst.embedClassesDirectory(new File(embedClassesDirectory));
                    } catch (ClassNotFoundException | IOException e) {
                        fail("Could not load class directory '" + embedClassesDirectory + "': " + e.getMessage());
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(embedClasses)) {
            for (String embedClass : embedClasses.split(LIST_SEPARATOR)) {
                if (StringUtils.isNotBlank(embedClass)) {
                    try {
                        Class<?> clazz = this.getClass().getClassLoader().loadClass(embedClass);
                        cst.embedClass(clazz);
                    } catch (ClassNotFoundException e) {
                        fail("Could not load class with name '" + embedClass + "': " + e.getMessage());
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(additionalBundleHeaders)) {
            for (String additionalBundleHeader : additionalBundleHeaders.split(LIST_SEPARATOR)) {
                if (StringUtils.isNotBlank(additionalBundleHeader)) {
                    // split up by name and value
                    int pos = additionalBundleHeader.indexOf(NAME_VALUE_SEPARATOR);
                    if (pos < 1 || pos >= additionalBundleHeader.length() -1) {
                        fail("Each entry given to property '" + PROPERTY_ADDITIONAL_BUNDLE_HEADERS + 
                             "' must have exactly the format <name>:<value>, but one entry is '" + additionalBundleHeader + "'.");
                    }
                    cst.addAdditionalBundleHeader(additionalBundleHeader.substring(0, pos), additionalBundleHeader.substring(pos+1));
                }
            }
        }
    }
}
