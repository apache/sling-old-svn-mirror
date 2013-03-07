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
package org.apache.sling.jmx.jolokia.impl;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.jolokia.osgi.servlet.JolokiaServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = true, label = "%jolokia.servlet.name", description = "%jolokia.servlet.description")
@Properties({ @Property(name = Constants.SERVICE_DESCRIPTION, value = "Sling Jolokia Servlet"),
        @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation") })
public class SlingJolokiaServlet {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Default value for the Jolokia servlet registration.
     */
    private static final String DEFAULT_JOLOKIA_ROOT = "/system/jolokia";

    /**
     * Name of the property to configure the location for the Jolokia servlet
     * registration. Default for the property is {@link #DEFAULT_JOLOKIA_ROOT}.
     */
    @Property(value = DEFAULT_JOLOKIA_ROOT)
    private static final String PROP_JOLOKIA_ROOT = "alias";

    private static final String[] DEFAULT_ALLOWED_USER_IDS = { "admin" };

    @Property(value = { "admin" }, unbounded=PropertyUnbounded.ARRAY)
    private static final String PROP_ALLOWED_USER_IDS = "allowed.userids";
    
    /**
     * The name of the service property of the registered dummy service to cause
     * the path to the DavEx servlet to not be subject to forced authentication.
     */
    private static final String PAR_AUTH_REQ = "sling.auth.requirements";

    @Reference
    private HttpService httpService;

    @Reference
    private AuthenticationSupport authSupport;

    /**
     * The path at which the Jolokia servlet has successfully been registered in
     * the {@link #activate(Map)} method. If this is <code>null</code> the
     * Jolokia servlet is not registered with the Http Service.
     */
    private String servletAlias;

    /**
     * The dummy service registration to convey to the Sling Authenticator that
     * everything under the alias must not be forcibly authenticated. This will
     * be <code>null</code> if the Jolokia servlet registration fails.
     */
    private ServiceRegistration dummyService;

    @Activate
    protected void activate(final BundleContext bundleContext, final Map<String, ?> config) {
        final String jolokiaRoot = OsgiUtil.toString(config.get(PROP_JOLOKIA_ROOT), DEFAULT_JOLOKIA_ROOT);
        final String[] allowedUserIds = OsgiUtil.toStringArray(config.get(PROP_ALLOWED_USER_IDS), DEFAULT_ALLOWED_USER_IDS);
        
        if (allowedUserIds.length == 0) {
            logger.warn("allowedUserId list is empty; all users will be allowed access to JMX data.");
        }
        
        final AuthHttpContext context = new AuthHttpContext(authSupport, Arrays.asList(allowedUserIds));

        // prepare servlet config
        final Dictionary<String, String> initProps = new Hashtable<String, String>();

        // register and handle registration failure
        try {
            this.httpService.registerServlet(jolokiaRoot, new JolokiaServlet(bundleContext), initProps, context);
            this.servletAlias = jolokiaRoot;

            java.util.Properties dummyServiceProperties = new java.util.Properties();
            dummyServiceProperties.put(Constants.SERVICE_VENDOR, config.get(Constants.SERVICE_VENDOR));
            dummyServiceProperties.put(Constants.SERVICE_DESCRIPTION,
                    "Helper for " + config.get(Constants.SERVICE_DESCRIPTION));
            dummyServiceProperties.put(PAR_AUTH_REQ, "-" + jolokiaRoot);
            this.dummyService = bundleContext.registerService("java.lang.Object", new Object(), dummyServiceProperties);
        } catch (Exception e) {
            
            logger.error("activate: Failed registering Jolokia Servlet at " + jolokiaRoot, e);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (this.dummyService != null) {
            this.dummyService.unregister();
            this.dummyService = null;
        }

        if (this.servletAlias != null) {
            this.httpService.unregister(servletAlias);
            this.servletAlias = null;
        }
    }
}
