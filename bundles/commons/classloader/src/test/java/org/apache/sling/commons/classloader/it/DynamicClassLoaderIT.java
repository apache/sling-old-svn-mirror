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
package org.apache.sling.commons.classloader.it;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.Constants.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.options.AbstractDelegateProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;


@RunWith(PaxExam.class)
public class DynamicClassLoaderIT {

    // the name of the system property providing the bundle file to be installed and tested
    private static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    @Inject
    protected BundleContext bundleContext;

    protected ClassLoader dynamicClassLoader;

    protected ServiceReference classLoaderManagerReference;

    /**
     * Helper method to get a service of the given type
     */
    @SuppressWarnings("unchecked")
	protected <T> T getService(Class<T> clazz) {
    	final ServiceReference ref = bundleContext.getServiceReference(clazz.getName());
    	assertNotNull("getService(" + clazz.getName() + ") must find ServiceReference", ref);
    	final T result = (T)(bundleContext.getService(ref));
    	assertNotNull("getService(" + clazz.getName() + ") must find service", result);
    	return result;
    }

    protected ClassLoader getDynamicClassLoader() {
        if ( classLoaderManagerReference == null || classLoaderManagerReference.getBundle() == null ) {
            dynamicClassLoader = null;
            classLoaderManagerReference = bundleContext.getServiceReference(DynamicClassLoaderManager.class.getName());
        }
        if ( dynamicClassLoader == null && classLoaderManagerReference != null ) {
            final DynamicClassLoaderManager dclm = (DynamicClassLoaderManager) bundleContext.getService(classLoaderManagerReference);
            if ( dclm != null ) {
                dynamicClassLoader = dclm.getDynamicClassLoader();
            }
        }
        return dynamicClassLoader;
    }

    @ProbeBuilder
    public TestProbeBuilder extendProbe(TestProbeBuilder builder) {
        builder.setHeader(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.apache.sling.commons.classloader");
        builder.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.ops4j.pax.exam,org.junit,javax.inject,org.ops4j.pax.exam.options");
        builder.setHeader("Bundle-ManifestVersion", "2");
        return builder;
    }

    @Configuration
    public static Option[] configuration() {
        final String bundleFileName = System.getProperty( BUNDLE_JAR_SYS_PROP );
        final File bundleFile = new File( bundleFileName );
        if ( !bundleFile.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from bundle file " + bundleFileName + " specified in the "
                + BUNDLE_JAR_SYS_PROP + " system property" );
        }

        return options(
            provision(
                CoreOptions.bundle( bundleFile.toURI().toString() ),
                mavenBundle( "org.ops4j.pax.tinybundles", "tinybundles", "1.0.0" ),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "2.1.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.2.14"),
                mavenBundle("org.ops4j.pax.url", "pax-url-mvn", "1.3.5")
             ),
             // below is instead of normal Pax Exam junitBundles() to deal
             // with build server issue
             new DirectURLJUnitBundlesOption(), 
             systemProperty("pax.exam.invoker").value("junit"),  
             bundle("link:classpath:META-INF/links/org.ops4j.pax.exam.invoker.junit.link")
        );
    }
        
    @Test
    public void testPackageAdminClassLoader() throws Exception {
        // check class loader
        assertNotNull(getDynamicClassLoader());

        final URL url = new URL(mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.1.0").getURL());
        final InputStream is = url.openStream();
        Bundle osgiBundle = null;
        try {
            osgiBundle = this.bundleContext.installBundle(url.toExternalForm(), is);
        } finally {
            try { is.close(); } catch ( final IOException ignore) {}
        }
        assertNotNull(osgiBundle);
        assertEquals(Bundle.INSTALLED, osgiBundle.getState());

        final String className = "org.apache.sling.commons.osgi.PropertiesUtil";

        // try to load class when bundle is in state install: should fail
        try {
            getDynamicClassLoader().loadClass(className);
            fail("Class should not be available");
        } catch (final ClassNotFoundException expected) {
            // expected
        }

        // force resolving of the bundle
        osgiBundle.getResource("/something");
        assertEquals(Bundle.RESOLVED, osgiBundle.getState());
        // try to load class when bundle is in state resolve: should fail
        try {
            getDynamicClassLoader().loadClass(className);
            fail("Class should not be available");
        } catch (final ClassNotFoundException expected) {
            // expected
        }

        // start bundle
        osgiBundle.start();
        assertEquals(Bundle.ACTIVE, osgiBundle.getState());
        // try to load class when bundle is in state activate: should work
        try {
            getDynamicClassLoader().loadClass(className);
        } catch (final ClassNotFoundException expected) {
            fail("Class should be available");
        }
    }

    /**
     * Clone of Pax Exam's JunitBundlesOption which uses a direct
     * URL to the SpringSource JUnit bundle to avoid some weird
     * repository issues on the Apache build server.
     */
    private static class DirectURLJUnitBundlesOption
        extends AbstractDelegateProvisionOption<DirectURLJUnitBundlesOption> {
    
        /**
         * Constructor.
         */
        public DirectURLJUnitBundlesOption(){
            super(
                bundle("http://repository.springsource.com/ivy/bundles/external/org.junit/com.springsource.org.junit/4.9.0/com.springsource.org.junit-4.9.0.jar")
            );
            noUpdate();
            startLevel(START_LEVEL_SYSTEM_BUNDLES);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("DirectURLJUnitBundlesOption{url=%s}", getURL());
        }
    
        /**
         * {@inheritDoc}
         */
        protected DirectURLJUnitBundlesOption itself() {
            return this;
        }
    
    }
}