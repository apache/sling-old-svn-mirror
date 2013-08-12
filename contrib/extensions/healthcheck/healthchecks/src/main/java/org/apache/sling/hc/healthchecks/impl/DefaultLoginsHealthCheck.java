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
package org.apache.sling.hc.healthchecks.impl;

import java.util.Arrays;
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
import org.apache.sling.hc.api.ResultLogEntry;
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
    private Map<String, String> info;
    
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
        info = new HealthCheckInfo(ctx.getProperties());
        logins = Arrays.asList(PropertiesUtil.toStringArray(ctx.getProperties().get(PROP_LOGINS), new String[] {}));
        log.info("Activated, logins={}", logins);
    }
    
    @Override
    public Result execute() {
        final Result result = new Result(log);
        int checked=0;
        int failures=0;
        
        for(String login : logins) {
            final String [] parts = login.split(":");
            if(parts.length != 2) {
                result.log(ResultLogEntry.LT_WARN, "Expected login in the form username:password, got " + login);
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
                    result.log(ResultLogEntry.LT_WARN_SECURITY, "Login as [" + username + "] succeeded, was expecting it to fail");
                } else {
                    result.log(ResultLogEntry.LT_DEBUG, "Login as [" + username + "] didn't throw an Exception but returned null Session");
                }
            } catch(RepositoryException re) {
                result.log(ResultLogEntry.LT_DEBUG, "Login as [" + username + "] failed, as expected");
            } finally {
                if(s != null) {
                    s.logout();
                }
            }
        }
        
        if(checked==0) {
            result.log(ResultLogEntry.LT_WARN, "Did not check any logins, configured logins=" + logins);
        } else if(failures != 0){
            result.log(ResultLogEntry.LT_WARN_SECURITY, "Checked " + checked + " logins, " + failures + " tests failed");
        } else {
            result.log(ResultLogEntry.LT_DEBUG, "Checked " + checked + " logins, all tests successful");
        }
        return result;
    }

    @Override
    public Map<String, String> getInfo() {
        return info;
    }
}