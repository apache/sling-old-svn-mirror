/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.testing.teleporter.client;

import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

/** Client-side TeleporterRule. Packages the
 *  test to run in a test bundle, installs the bundle,
 *  executes the test via the JUnit servlet, collects
 *  the results and uninstalls the bundle.  
 */
public class ClientSideTeleporter extends TeleporterRule {

    public static final String DEFAULT_TEST_SERVLET_PATH = "system/sling/junit";
    private DependencyAnalyzer dependencyAnalyzer;
    private int testReadyTimeoutSeconds = 20;
    private int webConsoleReadyTimeoutSeconds = 30;
    private int waitForServiceTimout = 10;
    private boolean enableLogging = false;
    private boolean preventToUninstallBundle = false;
    private File directoryForPersistingTestBundles = null;
    private String baseUrl;
    private String serverCredentials;
    private String testServletPath = DEFAULT_TEST_SERVLET_PATH;
    private final Set<Class<?>> embeddedClasses = new HashSet<Class<?>>();
    private final Map<String, String> additionalBundleHeaders = new HashMap<String, String>();
    
    private Logger log;
    
    public ClientSideTeleporter() {
        initLogger();
    }
    
    private InputStream buildTestBundle(Class<?> c, Collection<Class<?>> embeddedClasses, String bundleSymbolicName) throws IOException {
        final TinyBundle b = TinyBundles.bundle()
            .set(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName)
            .set("Sling-Test-Regexp", c.getName() + ".*")
            .set("Sling-Test-WaitForService-Timeout", Integer.toString(waitForServiceTimout))
            .add(c);

        for(Map.Entry<String, String> header : additionalBundleHeaders.entrySet()) {
            log.info("Add bundle header '{}' with value '{}'", header.getKey(), header.getValue());
            b.set(header.getKey(), header.getValue());
        }
        
        // enrich embedded classes by automatically detected dependencies
        for(Class<?> clz : dependencyAnalyzer.getDependencies(log)) {
            log.debug("Embed dependent class '{}' because it is referenced and in the allowed package prefixes", clz);
            b.add(clz);
        }
        
        // Embed specified classes
        for(Class<?> clz : embeddedClasses) {
            log.info("Embed class '{}'", clz);
            b.add(clz);
        }
        
        // Embed specified resources
        if(!embeddedResourcePaths.isEmpty()) {
            for(String path : embeddedResourcePaths) {
                final ClassResourceVisitor.Processor p = new ClassResourceVisitor.Processor() {
                    @Override
                    public void process(String resourcePath, InputStream resourceStream) throws IOException {
                        b.add(resourcePath, resourceStream);
                        log.info("Embed resource '{}'", resourcePath);
                    }
                    
                };
                new ClassResourceVisitor(getClass(), path).visit(p);
            }
        }
        
        return b.build(TinyBundles.withBnd());
    }
    
    public void setBaseUrl(String url) {
        baseUrl = url;
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() -1);
        }
    }

    @Override
    protected void setClassUnderTest(Class<?> c) {
        super.setClassUnderTest(c);
        dependencyAnalyzer = DependencyAnalyzer.forClass(classUnderTest);
    }
    
    /** Define how long to wait for our test to be ready on the server-side,
     *  after installing the test bundle */
    public void setTestReadyTimeoutSeconds(int tm) {
        testReadyTimeoutSeconds = tm;
    }
    
    /** Define how long to wait for the webconsole to be ready, before installing the test bundle */
    public void setWebConsoleReadyTimeoutSeconds (int tm) {
        webConsoleReadyTimeoutSeconds = tm;
    }
    
    /**
     * Define how long to wait to get a service reference.
     * This applies only on the server-side when using the {@link #getService(Class)} or {@link #getService(Class, String)} methods.
     */
    public void setWaitForServiceTimoutSeconds (int tm) {
        waitForServiceTimout = tm;
    }
    
    /** Set the credentials to use to install our test bundle on the server */
    public void setServerCredentials(String username, String password) {
        serverCredentials = username + ":" + password;
    }
    
    /**
     * @param testServletPath
     *            relative path to the Sling JUnit test servlet. If null, defaults to DEFAULT_TEST_SERVLET_PATH.
     */
    public void setTestServletPath(String testServletPath) {
        this.testServletPath = testServletPath == null ? DEFAULT_TEST_SERVLET_PATH : testServletPath;
    }

    /** Define a prefix for class names that can be embedded
     *  in the test bundle if the {@link DependencyAnalyzer} thinks
     *  they should. Overridden by {@link #excludeDependencyPrefix } if
     *  any conflicts arise.
     */
    public ClientSideTeleporter includeDependencyPrefix(String prefix) {
        dependencyAnalyzer.include(prefix);
        return this;
    }
    
    /** Define a prefix for class names that should not be embedded
     *  in the test bundle. Takes precedence over {@link #includeDependencyPrefix }.
     */
    public ClientSideTeleporter excludeDependencyPrefix(String prefix) {
        dependencyAnalyzer.exclude(prefix);
        return this;
    }
    
    /** Indicate that a specific class must be embedded in the test bundle. 
     *  In theory our DependencyAnalyzer should find which classes need to be
     *  embedded, but if that does not work this method can be used
     *  as a workaround.  
     */
    public ClientSideTeleporter embedClass(Class<?> c) {
        embeddedClasses.add(c);
        return this;
    }
    
    /** Set additional bundle headers on the generated test bundle */
    public void addAdditionalBundleHeader(String name, String value) {
        additionalBundleHeaders.put(name, value);
    }
    
    public Map<String, String> getAdditionalBundleHeaders() {
        return additionalBundleHeaders;
    }
    
    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
        this.initLogger();
    }

    public void setPreventToUninstallBundle(boolean preventToUninstallTestBundle) {
        this.preventToUninstallBundle = preventToUninstallTestBundle;
    }

    public void setDirectoryForPersistingTestBundles(File directoryForPersistingTestBundles) {
        this.directoryForPersistingTestBundles = directoryForPersistingTestBundles;
    }

    /** Embeds every class found in the given directory
     * 
     * @throws IOException */
    public void embedClassesDirectory(File classesDirectory) throws IOException, ClassNotFoundException {
        final Path start = classesDirectory.toPath();
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (file.getFileName().toString().endsWith(".class")) {
                    String className = start.relativize(file).toString().replace(file.getFileSystem().getSeparator(), ".");
                    // strip off extension
                    className = className.substring(0, className.length() - 6);
                    try {
                        Class<?> clazz = this.getClass().getClassLoader().loadClass(className);
                        embedClass(clazz);
                    } catch (ClassNotFoundException e) {
                        throw new IOException("Could not load class with name '" + className + "'", e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String installTestBundle(TeleporterHttpClient httpClient) throws MalformedURLException, IOException {
        final String bundleSymbolicName = getClass().getSimpleName() + "." + classUnderTest.getSimpleName();
        log.info("Building bundle '{}'", bundleSymbolicName, baseUrl);
        try (final InputStream bundle = buildTestBundle(classUnderTest, embeddedClasses, bundleSymbolicName)) {
            // optionally persist the test bundle
            if (directoryForPersistingTestBundles != null) {
                directoryForPersistingTestBundles.mkdirs();
                File bundleFile = new File(directoryForPersistingTestBundles, bundleSymbolicName + ".jar");
                log.info("Persisting test bundle in '{}'", bundleFile);
                try (OutputStream output = new FileOutputStream(bundleFile)) {
                    IOUtils.copy(bundle, output);
                }
                try (InputStream bundleInput = new BufferedInputStream(new FileInputStream(bundleFile))) {
                    log.info("Installing bundle '{}' to {}", bundleSymbolicName, baseUrl);
                    httpClient.installBundle(bundleInput, bundleSymbolicName, webConsoleReadyTimeoutSeconds);
                }
            } else {
                log.info("Installing bundle '{}' to {}", bundleSymbolicName, baseUrl);
                httpClient.installBundle(bundle, bundleSymbolicName, webConsoleReadyTimeoutSeconds);
            }
            httpClient.verifyCorrectBundleState(bundleSymbolicName, webConsoleReadyTimeoutSeconds);
        };
        return bundleSymbolicName;
    }

    private void initLogger() {
        if (enableLogging) {
            log = LoggerFactory.getLogger(ClientSideTeleporter.class);
        } else {
            log = NOPLogger.NOP_LOGGER;
        }
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        customize();
        initLogger();
        if(baseUrl == null) {
            fail("base URL is not set");
        }
        
        if(serverCredentials == null || serverCredentials.isEmpty()) {
            fail("server credentials are not set");
        }
        
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        final TeleporterHttpClient httpClient = new TeleporterHttpClient(baseUrl, testServletPath);
        httpClient.setCredentials(serverCredentials);
        
        // As this is not a ClassRule (which wouldn't map the test results correctly in an IDE)
        // we currently create and install a test bundle for every test method. It might be good
        // to optimize this, but as those test bundles are usually very small that doesn't seem
        // to be a real problem in terms of performance.
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                
                final String bundleSymbolicName = installTestBundle(httpClient);
                final String testPath = description.getClassName() + "/" + description.getMethodName();
                try {
                    httpClient.runTests(testPath, testReadyTimeoutSeconds);
                } finally {
                    if (!preventToUninstallBundle) {
                        log.info("Uninstalling bundle '{}' from {}", bundleSymbolicName, baseUrl);
                        httpClient.uninstallBundle(bundleSymbolicName, webConsoleReadyTimeoutSeconds);
                    } else {
                        log.info("Not uninstalling bundle '{}' from {} due to according configuration", bundleSymbolicName, baseUrl);
                    }
                }
            }
        };
    }
}
