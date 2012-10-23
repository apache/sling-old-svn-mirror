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

package org.apache.sling.jcr.jackrabbit.base.security;


import org.apache.jackrabbit.core.config.BeanConfig;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Map;
import java.util.Properties;

public class DelegatingLoginModule implements LoginModule {
    private static Logger logger = LoggerFactory.getLogger(DelegatingLoginModule.class);
    public static final String JAAS_CONFIG_ALGO_NAME = "JavaLoginConfig";
    private LoginModule delegate;
    private LoginContext loginContext;
    private boolean loginSucceeded;

    private String appName;
    private String delegateLoginModuleClass;
    private String providerName;

    private LoginException loginException;


    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
                           Map<String, ?> options) {
        Configuration config = null;
        try{
            config = Configuration.getInstance(JAAS_CONFIG_ALGO_NAME, null, providerName);
        }catch (NoSuchProviderException e){
            logger.debug("No provider "+providerName+"found so far",e);
        } catch (NoSuchAlgorithmException e) {
            logger.debug("No provider "+providerName+"found so far for fetching JAAS " +
                    "config with algorithm name "+JAAS_CONFIG_ALGO_NAME,e);
        }

        if(config != null){
            final Thread current = Thread.currentThread();
            final ClassLoader orig = current.getContextClassLoader();
            try {
                current.setContextClassLoader(DelegatingLoginModule.class.getClassLoader());
                loginContext = new LoginContext(appName, subject,callbackHandler, config);
            } catch (LoginException e) {
                loginException = e;
            } finally{
                current.setContextClassLoader(orig);
            }
        }else{
            //No support so far from OSGi so would use default logic used by Jackrabbit
            //to construct the LoginModule
            Properties p = new Properties();
            p.putAll(options);
            BeanConfig bc = new BeanConfig(delegateLoginModuleClass,p);
            LoginModuleConfig lmc = new LoginModuleConfig(bc);
            try {
                delegate = lmc.getLoginModule();
                delegate.initialize(subject,callbackHandler,sharedState,options);
                logger.info("No JAAS Configuration provider found would be directly invoking LoginModule {}",delegateLoginModuleClass);
            } catch (ConfigurationException e) {
                //Behaviour is same as org.apache.jackrabbit.core.security.authentication.LocalAuthContext.login()
                loginException = new LoginException(e.getMessage());
            }
        }
    }

    public boolean login() throws LoginException {
        assertState();

        if (loginContext == null) {
            return delegate.login();
        } else {
            loginContext.login();
            loginSucceeded = true;
            return true;
        }
    }

    public boolean commit() throws LoginException {
        assertState();

        if (loginContext == null) {
            return delegate.commit();
        } else {
            return loginSucceeded;
        }
    }

    public boolean abort() throws LoginException {
        assertState();

        if (loginContext == null) {
            return delegate.abort();
        } else {
            return loginSucceeded;
        }
    }

    public boolean logout() throws LoginException {
        assertState();

        if (loginContext == null) {
            return delegate.logout();
        } else {
            loginContext.logout();
            return true;
        }
    }

    private void assertState() throws LoginException {
        if(loginException != null){
            throw loginException;
        }
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setDelegateLoginModuleClass(String delegateLoginModuleClass) {
        this.delegateLoginModuleClass = delegateLoginModuleClass;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

}
