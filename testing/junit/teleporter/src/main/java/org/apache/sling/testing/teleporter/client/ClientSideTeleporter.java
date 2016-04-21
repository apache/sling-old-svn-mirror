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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;

/** Client-side TeleporterRule. Packages the
 *  test to run in a test bundle, installs the bundle,
 *  executes the test via the JUnit servlet, collects
 *  the results and uninstalls the bundle.  
 */
public class ClientSideTeleporter extends TeleporterRule {

    public static final String DEFAULT_TEST_SERVLET_PATH = "system/sling/junit";
    private DependencyAnalyzer dependencyAnalyzer;
    private int testReadyTimeoutSeconds = 5;
    private int webConsoleReadyTimeoutSeconds = 30;
    private String baseUrl;
    private String serverCredentials;
    private String testServletPath = DEFAULT_TEST_SERVLET_PATH;
    private final Set<Class<?>> embeddedClasses = new HashSet<Class<?>>();
    private final Map<String, String> additionalBundleHeaders = new HashMap<String, String>();
    
    private InputStream buildTestBundle(Class<?> c, Collection<Class<?>> embeddedClasses, String bundleSymbolicName) throws IOException {
        final TinyBundle b = TinyBundles.bundle()
            .set(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName)
            .set("Sling-Test-Regexp", c.getName() + ".*")
            .add(c);

        for(Map.Entry<String, String> header : additionalBundleHeaders.entrySet()) {
            b.set(header.getKey(), header.getValue());
        }
        
        // Embed specified classes
        for(Class<?> clz : embeddedClasses) {
            b.add(clz);
        }
        
        // Embed specified resources
        if(!embeddedResourcePaths.isEmpty()) {
            for(String path : embeddedResourcePaths) {
                final ClassResourceVisitor.Processor p = new ClassResourceVisitor.Processor() {
                    @Override
                    public void process(String resourcePath, InputStream resourceStream) throws IOException {
                        b.add(resourcePath, resourceStream);
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
    public Map<String, String> getAdditionalBundleHeaders() {
        return additionalBundleHeaders;
    }
    
    private String installTestBundle(TeleporterHttpClient httpClient) throws MalformedURLException, IOException {
        final SimpleDateFormat fmt = new SimpleDateFormat("HH-mm-ss-");
        final String bundleSymbolicName = getClass().getSimpleName() + "." + fmt.format(new Date()) + "." + UUID.randomUUID();
        final InputStream bundle = buildTestBundle(classUnderTest, embeddedClasses, bundleSymbolicName);
        httpClient.installBundle(bundle, bundleSymbolicName, webConsoleReadyTimeoutSeconds);
        return bundleSymbolicName;
    }
    
    @Override
    public Statement apply(final Statement base, final Description description) {
        customize();
        
        if(baseUrl == null) {
            fail("base URL is not set");
        }
        
        if(serverCredentials == null || serverCredentials.isEmpty()) {
            fail("server credentials are not set");
        }
        
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        for(Class<?> c : dependencyAnalyzer.getDependencies()) {
            embeddedClasses.add(c);
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
                    httpClient.uninstallBundle(bundleSymbolicName);
                }
            }
        };
    }
}