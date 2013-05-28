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
package org.apache.sling.ftpserver.impl;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = SlingConfiguration.NAME,
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE,
        label = "Apache Sling FTP Server",
        description = "Provides FTP Server access to the Apache Sling resources")
@Properties({
    @Property(
            name = SlingConfiguration.PROP_PORT,
            intValue = SlingConfiguration.PROP_PORT_DEFAULT,
            label = "Server Port",
            description = "Port for the FTP Server to listen on. "
                + "If zero a port is automatically selected (not recommended). Default value is "
                + SlingConfiguration.PROP_PORT_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_ANONYMOUS_LOGINS,
            boolValue = SlingConfiguration.PROP_ANONYMOUS_LOGINS_DEFAULT,
            label = "Server Port",
            description = "Is anonymous logins allowed at the server? " + "The default is "
                + SlingConfiguration.PROP_ANONYMOUS_LOGINS_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_MAX_ANONYMOUS_LOGINS,
            intValue = SlingConfiguration.PROP_MAX_ANONYMOUS_LOGINS_DEFAULT,
            label = "Server Port",
            description = "The maximum number of anonymous logins the server would allow at any given time. This property is only effective if anonymous login is enabled at all."
                + "The default is " + SlingConfiguration.PROP_MAX_ANONYMOUS_LOGINS_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_MAX_LOGIN_FAILURES,
            intValue = SlingConfiguration.PROP_MAX_LOGIN_FAILURES_DEFAULT,
            label = "Server Port",
            description = "The maximum number of time an user can fail to login before getting disconnected. "
                + "The default is " + SlingConfiguration.PROP_MAX_LOGIN_FAILURES_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_LOGIN_FAILURE_DELAY,
            intValue = SlingConfiguration.PROP_LOGIN_FAILURE_DELAY_DEFAUT,
            label = "Server Port",
            description = "The delay in number of milliseconds between login failures. Important to make brute force attacks harder. "
                + "The default is " + SlingConfiguration.PROP_LOGIN_FAILURE_DELAY_DEFAUT),
    @Property(
            name = SlingConfiguration.PROP_MAX_LOGINS,
            intValue = SlingConfiguration.PROP_MAX_LOGINS_DEFAULT,
            label = "Server Port",
            description = "Set the maximum number of concurrently logged in users. The default is "
                + SlingConfiguration.PROP_MAX_LOGINS_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_MAX_THREADS,
            intValue = SlingConfiguration.PROP_MAX_THREADS_DEFAULT,
            label = "Server Port",
            description = "Returns the maximum number of threads the server is allowed to create for processing client requests. "
                + "The default is " + SlingConfiguration.PROP_MAX_THREADS_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_ENABLED,
            boolValue = SlingConfiguration.PROP_ENABLED_DEFAULT,
            label = "Enable Users",
            description = "Wether repository users are enabled for FTP by default or not. If this property is set to true, users are by default enabled. "
                + "This property can be configured on a per user basis setting the boolean "
                + SlingUserManager.FTP_ENABLED
                + " user property. Default value is "
                + SlingConfiguration.PROP_ENABLED_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_MAX_IDLE_TIME,
            intValue = SlingConfiguration.PROP_MAX_IDLE_TIME_DEFAULT,
            label = "Default Idle Time",
            description = "The default value for maximum time the FTP session may be idle before the server closing it. "
                + "Setting this to zero disables idle session time (which is not recommended). "
                + "This property can be configured on a per user basis setting the numeric"
                + SlingUserManager.FTP_MAX_IDLE_TIME_SEC
                + " user property. The default value is "
                + SlingConfiguration.PROP_MAX_IDLE_TIME_DEFAULT + " seconds"),
    @Property(
            name = SlingConfiguration.PROP_MAX_CONCURRENT,
            intValue = SlingConfiguration.PROP_MAX_CONCURRENT_DEFAULT,
            label = "Default Concurrent Sessions",
            description = "The default value for the maximum number of concurrent sessions for a single user. "
                + "Setting this to zero disables this limitation (which is not recommended). "
                + "This property can be configured on a per user basis setting the numeric "
                + SlingUserManager.FTP_MAX_CONCURRENT + " user property. The default value is "
                + SlingConfiguration.PROP_MAX_CONCURRENT_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_MAX_CONCURRENT_IP,
            intValue = SlingConfiguration.PROP_MAX_CONCURRENT_IP_DEFAULT,
            label = "Default Concurrent Sessions per IP Address",
            description = "The default value for the maximum number of concurrent sessions for a single user from the same client IP address. "
                + "Setting this to zero disables this limitation (which is not recommended). "
                + "This property can be configured on a per user basis setting the numeric "
                + SlingUserManager.FTP_MAX_CONCURRENT_PER_IP
                + " user property. The default value is "
                + SlingConfiguration.PROP_MAX_CONCURRENT_IP_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_MAX_DOWNLOAD,
            intValue = SlingConfiguration.PROP_MAX_DOWNLOAD_DEFAULT,
            label = "Default Download Data Rate",
            description = "The default value for maximum file download data rate in 1000 bytes per second. "
                + "Setting this to zero disables this limitation. "
                + "This property can be configured on a per user basis setting the numeric "
                + SlingUserManager.FTP_MAX_DOWNLOAD_RATE + " user property. The default value is "
                + SlingConfiguration.PROP_MAX_DOWNLOAD_DEFAULT),
    @Property(
            name = SlingConfiguration.PROP_MAX_UPLOAD,
            intValue = SlingConfiguration.PROP_MAX_UPLOAD_DEFAULT,
            label = "Default Upload Data Rate",
            description = "The default value for maximum file upload data rate in 1000 bytes per second. "
                + "Setting this to zero disables this limitation. "
                + "This property can be configured on a per user basis setting the numeric "
                + SlingUserManager.FTP_MAX_UPLOAD_RATE + " user property. The default value is "
                + SlingConfiguration.PROP_MAX_UPLOAD_DEFAULT)
})
public class SlingFtpServer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory rrFactory;

    private SlingConfiguration configuration;

    private FtpServer ftpServer;

    private ServiceRegistration webConsolePlugin;

    @SuppressWarnings("serial")
    @Activate
    private void activate(final BundleContext bundleContext, final Map<String, Object> config) {

        // Consider not using the FtpServerFactory but directly create the
        // FtpServer from our own custom FtpServerContext

        this.configuration = new SlingConfiguration(config);
        UserManager userManager = new SlingUserManager(this.rrFactory, this.configuration);

        final ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(this.configuration.getPort());

        final SlingFtpletProxy wcPlugin = new SlingFtpletProxy();

        final FtpServerFactory factory = new FtpServerFactory();
        factory.setFileSystem(new SlingFileSystemFactory());
        factory.setUserManager(userManager);
        factory.setConnectionConfig(this.configuration);
        factory.addListener("default", listenerFactory.createListener());
        factory.setFtplets(new HashMap<String, Ftplet>() {
            {
                put("sling", wcPlugin);
            }
        });

        try {
            final FtpServer ftpServer = factory.createServer();
            ftpServer.start();
            this.ftpServer = ftpServer;
        } catch (FtpException e) {
            log.error("Cannot start FTP Server", e);
        }

        this.webConsolePlugin = bundleContext.registerService("javax.servlet.Servlet", new ServiceFactory() {

            public Object getService(Bundle bundle, ServiceRegistration registration) {
                final WebConsoleFtpLet tmp = new WebConsoleFtpLet();
                wcPlugin.setDelegatee(tmp);
                return tmp;
            }

            public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
                wcPlugin.setDelegatee(null);
            }

        }, new Hashtable<String, Object>() {
            {
                put("felix.webconsole.label", "ftpserver");
                put("felix.webconsole.title", "FTP Server");
                put("felix.webconsole.category", "Sling");
            }
        });
    }

    @Deactivate
    private void deactivate() {
        if (this.webConsolePlugin != null) {
            this.webConsolePlugin.unregister();
            this.webConsolePlugin = null;
        }

        if (this.ftpServer != null) {
            this.ftpServer.stop();
            this.ftpServer = null;
        }
    }
}
