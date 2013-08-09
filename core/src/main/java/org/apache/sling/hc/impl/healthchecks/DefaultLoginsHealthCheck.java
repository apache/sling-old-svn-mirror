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
import java.util.List;
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
    
    @Property(cardinality=500)
    public static final String PROP_LOGINS = "logins";
    private List<String> logins;
    
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
        logins = Arrays.asList(PropertiesUtil.toStringArray(ctx.getProperties().get(PROP_LOGINS), new String[] {}));
        
        info.put(Constants.HC_NAME, PropertiesUtil.toString(ctx.getProperties().get(Constants.HC_NAME), ""));
        info.put(Constants.HC_MBEAN_NAME, PropertiesUtil.toString(ctx.getProperties().get(Constants.HC_MBEAN_NAME), ""));
        info.put(Constants.HC_TAGS, 
                Arrays.asList(PropertiesUtil.toStringArray(ctx.getProperties().get(Constants.HC_TAGS), new String[] {})).toString());
        
        log.info("Activated, logins={}", logins);
    }
    
    @Override
    public Result execute(ResultLog log) {
        final Result result = new Result(this, log);
        int checked=0;
        int failures=0;
        
        for(String login : logins) {
            final String [] parts = login.split(":");
            if(parts.length != 2) {
                log.warn("Expected login in the form username:password, got {}", login);
                continue;
            }
            checked++;
            final String username = parts[0].trim();
            final String password = parts[1].trim();
            final Credentials creds = new SimpleCredentials(username, password.toCharArray());
            Session s = null;
            try {
                s = repository.login(creds);
                if(s != null) {
                    failures++;
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
        }
        
        if(checked==0) {
            log.warn("Did not check any logins, configured logins={}", logins);
        } else if(failures != 0){
            log.warn("Checked {} logins, {} tests failed", checked, failures);
        } else {
            log.debug("Checked {} logins, all tests successful", checked);
        }
        return result;
    }

    @Override
    public Map<String, String> getInfo() {
        return info;
    }
}