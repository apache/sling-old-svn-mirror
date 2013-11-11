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
package org.apache.sling.event.it;


import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.impl.jobs.JobManagerConfiguration;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupMode;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public abstract class AbstractJobHandlingTest {

    private static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    protected static final int DEFAULT_TEST_TIMEOUT = 1000*60*5;

    @Inject
    protected EventAdmin eventAdmin;

    @Inject
    protected ConfigurationAdmin configAdmin;

    @Inject
    protected BundleContext bc;

    @Configuration
    public Option[] config() {
        final String bundleFileName = System.getProperty( BUNDLE_JAR_SYS_PROP );
        final File bundleFile = new File( bundleFileName );
        if ( !bundleFile.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from bundle file " + bundleFileName + " specified in the "
                + BUNDLE_JAR_SYS_PROP + " system property" );
        }

        String localRepo = System.getProperty("maven.repo.local", "");

        return options(
                when( localRepo.length() > 0 ).useOptions(
                        systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
                ),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.xml", "1.0.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.transaction", "1.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.activation", "1.0.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.ws", "1.0.2"),

                mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "3.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice", "1.0.2"),

                mavenBundle("org.slf4j", "slf4j-api", "1.6.4"),
                mavenBundle("org.slf4j", "jcl-over-slf4j", "1.6.4"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.6.4"),

                mavenBundle("commons-io", "commons-io", "1.4"),
                mavenBundle("commons-fileupload", "commons-fileupload", "1.2.2"),
                mavenBundle("commons-collections", "commons-collections", "3.2.1"),
                mavenBundle("commons-codec", "commons-codec", "1.6"),
                mavenBundle("commons-lang", "commons-lang", "2.5"),

                mavenBundle("org.apache.geronimo.bundles", "commons-httpclient", "3.1_1"),
                mavenBundle("org.apache.tika", "tika-core", "1.2"),
                mavenBundle("org.apache.tika", "tika-bundle", "1.2"),

                mavenBundle("org.apache.felix", "org.apache.felix.http.jetty", "2.2.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.2.14"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.8.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.6.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.inventory", "1.0.0"),
//                mavenBundle("org.apache.felix", "org.apache.felix.metatype", "1.0.6"),

                mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.2.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.json", "2.0.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.mime", "2.1.4"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.classloader", "1.3.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.scheduler", "2.4.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.threads", "3.1.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.launchpad.api", "1.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.1.4"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.api", "1.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.standalone", "1.0.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.api", "2.4.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.settings", "1.2.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.resourceresolver", "1.0.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.adapter", "2.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.resource", "2.2.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.classloader", "3.1.12"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.contentloader", "2.1.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.engine", "2.2.6"),

                mavenBundle("org.apache.sling", "org.apache.sling.jcr.jcr-wrapper", "2.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.api", "2.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.base", "2.1.2"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-api", "2.4.2"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-commons", "2.4.2"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-spi", "2.4.2"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-spi-commons", "2.4.2"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-rmi", "2.4.2"),
                mavenBundle("org.apache.derby", "derby", "10.5.3.0_1"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.jackrabbit.server", "2.1.2"),

                mavenBundle("org.apache.sling", "org.apache.sling.testing.tools", "1.0.6"),
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi", "4.1.2"),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.1.2"),

                CoreOptions.bundle( bundleFile.toURI().toString() ),

                junitBundles()
           );
    }

    protected JobManager getJobManager() {
        final ServiceReference sr = this.bc.getServiceReference(JobManager.class.getName());
        return (JobManager)this.bc.getService(sr);
    }

    protected void sleep(final long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void setup() throws IOException {
        // set load delay to 3 sec
        final org.osgi.service.cm.Configuration c2 = this.configAdmin.getConfiguration("org.apache.sling.event.impl.jobs.jcr.PersistenceHandler", null);
        Dictionary<String, Object> p2 = new Hashtable<String, Object>();
        p2.put(JobManagerConfiguration.PROPERTY_BACKGROUND_LOAD_DELAY, 3L);
        c2.update(p2);

        final StartupHandler handler = new StartupHandler() {

            @Override
            public void waitWithStartup(boolean flag) {
            }

            @Override
            public boolean isFinished() {
                return true;
            }

            @Override
            public StartupMode getMode() {
                return StartupMode.INSTALL;
            }
        };
        this.bc.registerService(StartupHandler.class.getName(), handler, null);
    }

    public void cleanup() {
        final ServiceReference ref = this.bc.getServiceReference(ResourceResolverFactory.class.getName());
        final ResourceResolverFactory factory = (ResourceResolverFactory) this.bc.getService(ref);
        ResourceResolver resolver = null;
        try {
            resolver = factory.getAdministrativeResourceResolver(null);
            final Resource rsrc = resolver.getResource(JobManagerConfiguration.DEFAULT_REPOSITORY_PATH);
            if ( rsrc != null ) {
                resolver.delete(rsrc);
                resolver.commit();
            }
        } catch ( final LoginException le ) {
            // ignore
        } catch (final PersistenceException e) {
            // ignore
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
    }

    /**
     * Helper method to register an event handler
     */
    protected ServiceRegistration registerEventHandler(final String topic,
            final EventHandler handler) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, topic);
        final ServiceRegistration reg = this.bc.registerService(EventHandler.class.getName(),
                handler, props);
        return reg;
    }

    /**
     * Helper method to register a job consumer
     */
    protected ServiceRegistration registerJobConsumer(final String topic,
            final JobConsumer handler) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(JobConsumer.PROPERTY_TOPICS, topic);
        final ServiceRegistration reg = this.bc.registerService(JobConsumer.class.getName(),
                handler, props);
        return reg;
    }

    /**
     * Helper method to register a job executor
     */
    protected ServiceRegistration registerJobExecutor(final String topic,
            final JobExecutor handler) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(JobConsumer.PROPERTY_TOPICS, topic);
        final ServiceRegistration reg = this.bc.registerService(JobExecutor.class.getName(),
                handler, props);
        return reg;
    }

    /**
     * Helper method to remove a configuration
     */
    protected void removeConfiguration(final String pid) throws IOException {
        if ( pid != null ) {
            final org.osgi.service.cm.Configuration cfg = this.configAdmin.getConfiguration(pid, null);
            cfg.delete();
        }
    }
}
