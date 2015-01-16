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

package org.apache.sling.auth.form.impl.jaas;

import java.util.Dictionary;

import javax.security.auth.spi.LoginModule;

import org.apache.felix.jaas.LoginModuleFactory;
import org.apache.sling.auth.form.impl.FormAuthenticationHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaasHelper {

    private static final Logger log = LoggerFactory.getLogger(JaasHelper.class);

    private final FormAuthenticationHandler authHandler;

    /**
     * login module service registration
     */
    private final ServiceRegistration factoryRegistration;

    /**
     * Opens/Initializes the helper and registers the login module factory (LMF) service if possible.
     *
     * @param ctx        the bundle context
     * @param properties properties that contain the jaas related LMF service properties.
     */
    public JaasHelper(FormAuthenticationHandler authHandler, BundleContext ctx, Dictionary properties) {
        this.authHandler = authHandler;
        // we dynamically register the LoginModuleFactory for the case we detect a login module.
        if (hasSSOLoginModule(ctx)) {
            factoryRegistration = registerLoginModuleFactory(ctx, properties);
        } else {
            factoryRegistration = null;
        }
    }

    /**
     * Checks if JAAS support is enabled and the SSO login module is present.
     *
     * @return {@code true} if JAAS support is enabled.
     */
    public boolean enabled() {
        return factoryRegistration != null;
    }


    /**
     * Closes this helper and unregisters the login module factory if needed.
     */
    public void close() {
        if (factoryRegistration != null) {
            factoryRegistration.unregister();
        }
    }

    private ServiceRegistration registerLoginModuleFactory(BundleContext ctx, Dictionary properties) {
        ServiceRegistration reg = null;
        try {
            java.util.Properties props = new java.util.Properties();
            final String desc = "LoginModule Support for FormAuthenticationHandler";
            props.put(Constants.SERVICE_DESCRIPTION, desc);
            props.put(Constants.SERVICE_VENDOR, ctx.getBundle().getHeaders().get(Constants.BUNDLE_VENDOR));

            props.put(LoginModuleFactory.JAAS_RANKING, properties.get(LoginModuleFactory.JAAS_RANKING));
            props.put(LoginModuleFactory.JAAS_CONTROL_FLAG, properties.get(LoginModuleFactory.JAAS_CONTROL_FLAG));
            props.put(LoginModuleFactory.JAAS_REALM_NAME, properties.get(LoginModuleFactory.JAAS_REALM_NAME));
            reg = ctx.registerService(LoginModuleFactory.class.getName(),
                    new LoginModuleFactory() {
                        public LoginModule createLoginModule() {
                            return new FormLoginModule(authHandler);
                        }

                        @Override
                        public String toString() {
                            return desc + " (" +FormLoginModule.class.getName()+")";
                        }
                    },
                    props
            );
            log.info("Registered FormLoginModuleFactory");
        } catch (Throwable e) {
            log.error("unable to create an register the SSO login module factory", e);
        }
        return reg;
    }

    /**
     * Checks if the {@link org.apache.sling.auth.form.impl.jaas.FormLoginModule} is available. This would not be the case
     * in an non-oak setup. Note this only checks if the login module can be loaded, not if it is actually enabled
     * in the jaas config.
     *
     * @return {@code true} if the SSOLoginModule is available.
     */
    private static boolean hasSSOLoginModule(BundleContext ctx) {
        try {
            ctx.getBundle().loadClass("org.apache.sling.auth.form.impl.jaas.FormLoginModule");
            log.debug("FormLoginModule available.");
            return true;
        } catch (Throwable e) {
            log.debug("no FormLoginModule available.", e);
        }
        return false;
    }
}
