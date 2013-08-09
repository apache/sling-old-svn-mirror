/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.impl.healthchecks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.Constants;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks that Sling default logins fail.
 *  Used to verify that those logins are disabled on production systems */
@Component(
        name="org.apache.sling.hc.DefaultLoginsHealthCheck",
        configurationFactory=true, 
        policy=ConfigurationPolicy.REQUIRE, 
        metatype=true)
@Service
public class DefaultLoginsHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<String, String> info = new HashMap<String, String>();
    private String username;
    private String password;
    
    @Property
    public static final String PROP_USERNAME = "username";
    
    @Property
    public static final String PROP_PASSWORD = "password";
    
    @Property(cardinality=50)
    public static final String PROP_TAGS = Constants.HC_TAGS;
    
    @Property
    public static final String PROP_NAME = Constants.HC_NAME;
    
    @Property
    public static final String PROP_MBEAN_NAME = Constants.HC_MBEAN_NAME;
    
    @Reference
    private SlingRepository repository;
    
    @Activate
    public void activate(ComponentContext ctx) {
        username = PropertiesUtil.toString(ctx.getProperties().get(PROP_USERNAME), "");
        password = PropertiesUtil.toString(ctx.getProperties().get(PROP_PASSWORD), "");
        
        info.put(PROP_USERNAME, username);
        info.put(Constants.HC_NAME, PropertiesUtil.toString(ctx.getProperties().get(Constants.HC_NAME), ""));
        info.put(Constants.HC_MBEAN_NAME, PropertiesUtil.toString(ctx.getProperties().get(Constants.HC_MBEAN_NAME), ""));
        info.put(Constants.HC_TAGS, 
                Arrays.asList(PropertiesUtil.toStringArray(ctx.getProperties().get(Constants.HC_TAGS), new String[] {})).toString());
        
        log.info("Activated, username={}", username);
    }
    
    @Override
    public Result execute(ResultLog log) {
        final Result result = new Result(this, log);
        final Credentials creds = new SimpleCredentials(username, password.toCharArray());
        Session s = null;
        try {
            s = repository.login(creds);
            if(s != null) {
                log.warn("Login as [{}] succeeded, was expecting it to fail", username);
            } else {
                log.debug("Login as [{}] didn't throw an Exception but returned null Session", username);
            }
        } catch(RepositoryException re) {
            log.debug("Login as [{}] failed, as expected", username);
        } finally {
            if(s != null) {
                s.logout();
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getInfo() {
        return info;
    }
}