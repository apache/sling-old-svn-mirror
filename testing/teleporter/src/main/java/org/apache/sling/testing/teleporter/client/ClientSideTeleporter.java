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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;

public class ClientSideTeleporter extends TeleporterRule {

    // TODO All this should be configurable with Options
    private final int testReadyTimeoutSeconds = 5;
    private String baseUrl;
    private final Set<String> dependencyAnalyzerIncludes = new HashSet<String>();
    private final Set<String> dependencyAnalyzerExcludes = new HashSet<String>();
    private final Set<Class<?>> embeddedClasses = new HashSet<Class<?>>();

    private InputStream buildTestBundle(Class<?> c, Collection<Class<?>> embeddedClasses, String bundleSymbolicName) {
        final TinyBundle b = TinyBundles.bundle()
            .set(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName)
            .set("Sling-Test-Regexp", c.getName() + ".*")
            .add(c);
        for(Class<?> clz : embeddedClasses) {
            b.add(clz);
        }
        return b.build(TinyBundles.withBnd());
    }
    
    protected void setBaseUrl(String url) {
        baseUrl = url;
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() -1);
        }
    }
    
    public void embedClass(Class<?> c) {
        embeddedClasses.add(c);
    }
    
    private String installTestBundle(TeleporterHttpClient httpClient) throws MalformedURLException, IOException {
        final SimpleDateFormat fmt = new SimpleDateFormat("HH-mm-ss-");
        final String bundleSymbolicName = getClass().getSimpleName() + "." + fmt.format(new Date()) + "." + UUID.randomUUID();
        final InputStream bundle = buildTestBundle(classUnderTest, embeddedClasses, bundleSymbolicName);
        httpClient.installBundle(bundle, bundleSymbolicName);
        return bundleSymbolicName;
    }
    
    @Override
    public Statement apply(final Statement base, final Description description) {
        if(baseUrl == null) {
            // TODO - need a more flexible mechanism + need to wait for Sling to be ready
            final String propName = "launchpad.http.server.url";
            baseUrl = System.getProperty(propName);
            if(baseUrl == null || baseUrl.isEmpty()) {
                fail("Missing system property " + propName);
            }
        }
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        final DependencyAnalyzer da = DependencyAnalyzer.forClass(classUnderTest);
        for(String include : dependencyAnalyzerIncludes) {
            da.include(include);
        }
        for(String exclude : dependencyAnalyzerExcludes) {
            da.exclude(exclude);
        }
        for(Class<?> c : da.getDependencies()) {
            embeddedClasses.add(c);
        }

        final TeleporterHttpClient httpClient = new TeleporterHttpClient(baseUrl);
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
