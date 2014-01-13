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
package org.apache.sling.hc.support.impl;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks that Sling default logins fail.
 *  Used to verify that those logins are disabled on production systems */
@Component(
        name="org.apache.sling.hc.support.DefaultLoginsHealthCheck",
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE,
        metatype=true,
        label="Apache Sling Default Logins Health Check",
        description="Expects default logins to fail, used to verify " +
                "that they are disabled on production systems")
@Properties({
    @Property(name=HealthCheck.NAME,
            label="Health Check Name", description="Name of this Health Check service."),
    @Property(name=HealthCheck.TAGS, unbounded=PropertyUnbounded.ARRAY,
             label="Health Check tags", description="List of tags for this Health Check service, used to select " +
               "subsets of Health Check services for execution"),
    @Property(name=HealthCheck.MBEAN_NAME,
             label="MBean Name", description="Name of the MBean to create for this Health Check.")
})
@Service(value=HealthCheck.class)
public class DefaultLoginsHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(unbounded=PropertyUnbounded.ARRAY,
            label="Login credentials",
            description="Which credentials to check. Each one is in the format \"user:password\" " +
                "like \"admin:admin\" for example. Do *not* put any confidential passwords here, the goal " +
                "is just to check that the default/demo logins, which passwords are known anyway, are disabled.")
    private static final String PROP_LOGINS = "logins";

    private List<String> logins;

    @Reference
    private SlingRepository repository;

    @Activate
    public void activate(ComponentContext ctx) {
        logins = Arrays.asList(PropertiesUtil.toStringArray(ctx.getProperties().get(PROP_LOGINS), new String[] {}));
        log.info("Activated, logins={}", logins);
    }

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        int checked=0;
        int failures=0;

        for(String login : logins) {
            final String [] parts = login.split(":");
            if(parts.length != 2) {
                resultLog.warn("Expected login in the form username:password, got [{}]", login);
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
                    resultLog.warn("Login as [{}] succeeded, was expecting it to fail", username);
                } else {
                    resultLog.debug("Login as [{}] didn't throw an Exception but returned null Session", username);
                }
            } catch(RepositoryException re) {
                resultLog.debug("Login as [{}] failed, as expected", username);
            } finally {
                if(s != null) {
                    s.logout();
                }
            }
        }

        if(checked==0) {
            resultLog.warn("Did not check any logins, configured logins={}", logins);
        } else if(failures != 0){
            resultLog.warn("Checked {} logins, {} failures", checked, failures);
        } else {
            resultLog.debug("Checked {} logins, all successful", checked, failures);
        }
        return new Result(resultLog);
    }
}