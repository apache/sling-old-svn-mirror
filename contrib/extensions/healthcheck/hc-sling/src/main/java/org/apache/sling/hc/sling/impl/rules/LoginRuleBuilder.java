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
package org.apache.sling.hc.sling.impl.rules;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.SystemAttribute;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;

/** Creates {@link Rule} to check that specific credentials do not allow for
 *  logging in to a {@link SlingRepository}. Can be used to verify 
 *  that default passwords have been disabled on production systems.
 *  Do NOT use any secret credentials when configuring this rule, it is
 *  only meant to check that well-known demo credentials are disabled on
 *  production systems.   
 */
@Component
@Service(value=RuleBuilder.class)
public class LoginRuleBuilder implements RuleBuilder {

    public static final String NAMESPACE = "sling";
    public static final String RULE_NAME = "loginfails";
    
    @Reference
    private SlingRepository repository;
    
    private class LoginResultSystemAttribute implements SystemAttribute {

        private final String username;
        private final String password;
        
        LoginResultSystemAttribute(String username, String password) {
            this.username= username;
            this.password = password;
        }
        
        @Override
        public Object getValue(Logger logger) {
            final Credentials creds = new SimpleCredentials(username, password.toCharArray());
            Session s = null;
            try {
                s = repository.login(creds);
                logger.warn("Login as user [{}] successful, should have failed", username);
            } catch(RepositoryException rex) {
                logger.debug("Login as user [{}] failed as expected", username);
            } finally {
                if(s != null) {
                    s.logout();
                }
            }
            return null;
        }
        
        @Override
        public String toString() {
            return "Expect login as user [" + username + "] to fail";
        }
    }
    
    @Override
    public Rule buildRule(String namespace, String ruleName, String qualifier, String expression) {
        if(!NAMESPACE.equals(namespace) || !RULE_NAME.equals(ruleName) || qualifier == null) {
            return null;
        }
        
        // Qualifier must be username#password
        final String [] creds = qualifier.split("#");
        if(creds.length != 2) {
            return null;
        }
        
        return new Rule(new LoginResultSystemAttribute(creds[0], creds[1]), expression);
    }
}
