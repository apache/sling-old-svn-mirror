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


import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public abstract class AbstractJobHandlingTest {

    private static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    /** The property containing the build directory. */
    private static final String SYS_PROP_BUILD_DIR = "bundle.build.dir";

    private static final String DEFAULT_BUILD_DIR = "target";

    private static final String PORT_CONFIG = "org.osgi.service.http.port";

    protected static final int DEFAULT_TEST_TIMEOUT = 1000*60*5;

    @Inject
    protected EventAdmin eventAdmin;

    @Inject
    protected ConfigurationAdmin configAdmin;

    @Inject
    protected BundleContext bc;

    protected List<ServiceRegistration<?>> registrations = new ArrayList<>();

    @Configuration
    public Option[] config() {
        final String buildDir = System.getProperty(SYS_PROP_BUILD_DIR, DEFAULT_BUILD_DIR);
        final String bundleFileName = System.getProperty( BUNDLE_JAR_SYS_PROP );
        final File bundleFile = new File( bundleFileName );
        if ( !bundleFile.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from bundle file " + bundleFileName + " specified in the "
                + BUNDLE_JAR_SYS_PROP + " system property" );
        }

        String localRepo = System.getProperty("maven.repo.local", "");

        return options(
                frameworkProperty("sling.home").value(new File(buildDir + File.separatorChar + "sling_" + System.currentTimeMillis()).getAbsolutePath()),
                when( localRepo.length() > 0 ).useOptions(
                        systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
                ),
                when( System.getProperty(PORT_CONFIG) != null ).useOptions(
                        systemProperty(PORT_CONFIG).value(System.getProperty(PORT_CONFIG))),
                systemProperty("pax.exam.osgi.unresolved.fail").value("true"),

                // logging
                systemProperty("pax.exam.logging").value("none"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "4.0.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice", "1.0.6"),
                mavenBundle("org.slf4j", "slf4j-api", "1.7.13"),
                mavenBundle("org.slf4j", "jcl-over-slf4j", "1.7.13"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.13"),

                mavenBundle("commons-io", "commons-io", "2.4"),
                mavenBundle("commons-fileupload", "commons-fileupload", "1.3.1"),
                mavenBundle("commons-collections", "commons-collections", "3.2.2"),
                mavenBundle("commons-codec", "commons-codec", "1.10"),
                mavenBundle("commons-lang", "commons-lang", "2.6"),
                mavenBundle("commons-pool", "commons-pool", "1.6"),

                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.concurrent", "1.3.4_1"),

                mavenBundle("org.apache.geronimo.bundles", "commons-httpclient", "3.1_1"),
                mavenBundle("org.apache.tika", "tika-core", "1.9"),
                mavenBundle("org.apache.tika", "tika-bundle", "1.9"),

                // infrastructure
                mavenBundle("org.apache.felix", "org.apache.felix.http.servlet-api", "1.1.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.4.4"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.8"),
                mavenBundle("org.apache.felix", "org.apache.felix.inventory", "1.0.4"),
                mavenBundle("org.apache.felix", "org.apache.felix.metatype", "1.1.2"),

                // sling
                mavenBundle("org.apache.sling", "org.apache.sling.settings", "1.3.8"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.3.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.json", "2.0.16"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.mime", "2.1.8"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.classloader", "1.3.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.scheduler", "2.4.14"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.threads", "3.2.4"),

                mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.3.12"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.api", "1.0.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.standalone", "1.0.2"),

                mavenBundle("org.apache.sling", "org.apache.sling.api", "2.9.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.resourceresolver", "1.2.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.adapter", "2.1.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.resource", "2.5.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.classloader", "3.2.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.contentloader", "2.1.8"),
                mavenBundle("org.apache.sling", "org.apache.sling.engine", "2.3.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.serviceusermapper", "1.2.2"),

                mavenBundle("org.apache.sling", "org.apache.sling.jcr.jcr-wrapper", "2.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.api", "2.3.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.base", "2.3.0"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-api", "2.10.1"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-commons", "2.10.1"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-spi", "2.10.1"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-spi-commons", "2.10.1"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-rmi", "2.10.1"),
                mavenBundle("org.apache.derby", "derby", "10.5.3.0_1"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.jackrabbit.server", "2.3.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.testing.tools", "1.0.6"),
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi", "4.1.2"),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.1.2"),

                CoreOptions.bundle( bundleFile.toURI().toString() ),

                junitBundles()
           );
    }

    protected JobManager getJobManager() {
        JobManager result = null;
        int count = 0;
        do {
            final ServiceReference<JobManager> sr = this.bc.getServiceReference(JobManager.class);
            if ( sr != null ) {
                result = this.bc.getService(sr);
            } else {
                count++;
                if ( count == 10 ) {
                    break;
                }
                sleep(500);
            }

        } while ( result == null );
        return result;
    }

    protected void sleep(final long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // ignore
        }
    }

    public void setup() throws IOException {
        // set load delay to 3 sec
        final org.osgi.service.cm.Configuration c2 = this.configAdmin.getConfiguration("org.apache.sling.event.impl.jobs.jcr.PersistenceHandler", null);
        Dictionary<String, Object> p2 = new Hashtable<String, Object>();
        p2.put(JobManagerConfiguration.PROPERTY_BACKGROUND_LOAD_DELAY, 3L);
        c2.update(p2);
    }

    private int deleteCount;

    private void delete(final Resource rsrc )
    throws PersistenceException {
        final ResourceResolver resolver = rsrc.getResourceResolver();
        for(final Resource child : rsrc.getChildren()) {
            delete(child);
        }
        resolver.delete(rsrc);
        deleteCount++;
        if ( deleteCount >= 20 ) {
            resolver.commit();
            deleteCount = 0;
        }
    }

    public void cleanup() {
        // clean job area
        final ServiceReference<ResourceResolverFactory> ref = this.bc.getServiceReference(ResourceResolverFactory.class);
        final ResourceResolverFactory factory = this.bc.getService(ref);
        ResourceResolver resolver = null;
        try {
            resolver = factory.getAdministrativeResourceResolver(null);
            final Resource rsrc = resolver.getResource("/var/eventing");
            if ( rsrc != null ) {
                delete(rsrc);
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
        // unregister all services
        for(final ServiceRegistration<?> reg : this.registrations) {
            reg.unregister();
        }
        this.registrations.clear();

        // remove all configurations
        try {
            final org.osgi.service.cm.Configuration[] cfgs = this.configAdmin.listConfigurations(null);
            if ( cfgs != null ) {
                for(final org.osgi.service.cm.Configuration c : cfgs) {
                    try {
                        c.delete();
                    } catch (final IOException io) {
                        // ignore
                    }
                }
            }
        } catch (final IOException io) {
            // ignore
        } catch (final InvalidSyntaxException e) {
            // ignore
        }
        this.sleep(1000);
    }

    /**
     * Helper method to register an event handler
     */
    protected ServiceRegistration<EventHandler> registerEventHandler(final String topic,
            final EventHandler handler) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, topic);
        final ServiceRegistration<EventHandler> reg = this.bc.registerService(EventHandler.class,
                handler, props);
        this.registrations.add(reg);
        return reg;
    }

    protected long getConsumerChangeCount() {
        long result = -1;
        try {
            final Collection<ServiceReference<PropertyProvider>> refs = this.bc.getServiceReferences(PropertyProvider.class, "(changeCount=*)");
            if ( !refs.isEmpty() ) {
                result = (Long)refs.iterator().next().getProperty("changeCount");
            }
        } catch ( final InvalidSyntaxException ignore ) {
            // ignore
        }
        return result;
    }

    protected void waitConsumerChangeCount(final long minimum) {
        do {
            final long cc = getConsumerChangeCount();
            if ( cc >= minimum ) {
                // we need to wait for the topology events (TODO)
                sleep(200);
                return;
            }
            sleep(50);
        } while ( true );
    }

    /**
     * Helper method to register a job consumer
     */
    protected ServiceRegistration<JobConsumer> registerJobConsumer(final String topic,
            final JobConsumer handler) {
        long cc = this.getConsumerChangeCount();
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(JobConsumer.PROPERTY_TOPICS, topic);
        final ServiceRegistration<JobConsumer> reg = this.bc.registerService(JobConsumer.class,
                handler, props);
        this.registrations.add(reg);
        this.waitConsumerChangeCount(cc + 1);
        return reg;
    }

    /**
     * Helper method to register a job executor
     */
    protected ServiceRegistration<JobExecutor> registerJobExecutor(final String topic,
            final JobExecutor handler) {
        long cc = this.getConsumerChangeCount();
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(JobConsumer.PROPERTY_TOPICS, topic);
        final ServiceRegistration<JobExecutor> reg = this.bc.registerService(JobExecutor.class,
                handler, props);
        this.registrations.add(reg);
        this.waitConsumerChangeCount(cc + 1);
        return reg;
    }

    protected void unregister(final ServiceRegistration<?> reg) {
        if ( reg != null ) {
            this.registrations.remove(reg);
            reg.unregister();
        }
    }
}
