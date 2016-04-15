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
package org.apache.sling.commons.messaging.mail;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Dictionary;

import javax.inject.Inject;

import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

public abstract class MailTestSupport {

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected ConfigurationAdmin configurationAdmin;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public MailTestSupport() {
    }

    protected synchronized int findFreePort() {
        try {
            final ServerSocket serverSocket = new ServerSocket(0);
            final int port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void createFactoryConfiguration(final String factoryPid, final Dictionary<String, Object> properties) throws IOException, InterruptedException {
        final org.osgi.service.cm.Configuration configuration = configurationAdmin.createFactoryConfiguration(factoryPid);
        configuration.setBundleLocation(null);
        configuration.update(properties);
        Thread.sleep(1000);
        logger.debug("configuration: {}", configurationAdmin.getConfiguration(factoryPid));
    }

    protected <T> T getService(Class<T> type) {
        final ServiceReference<T> serviceReference = bundleContext.getServiceReference(type);
        return bundleContext.getService(serviceReference);
    }

    protected Option[] baseConfiguration() {
        final String filename = System.getProperty("bundle.filename");
        return options(
            junitBundles(),
            provision(
                wrappedBundle(mavenBundle().groupId("org.subethamail").artifactId("subethasmtp").versionAsInProject()),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr").versionAsInProject(),
                mavenBundle().groupId("com.sun.mail").artifactId("javax.mail").versionAsInProject(),
                mavenBundle().groupId("javax.mail").artifactId("javax.mail-api").versionAsInProject(),
                mavenBundle().groupId("org.apache.commons").artifactId("commons-email").versionAsInProject(),
                mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
                mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.messaging").versionAsInProject(),
                mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.threads").versionAsInProject(),
                bundle("reference:file:" + filename)
            )
        );
    }

}
