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
package org.apache.sling.muppet.sling.impl.rules;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.api.RuleBuilder;
import org.apache.sling.muppet.api.SystemAttribute;

/** Creates {@link Rule} to check if specific credentials allow for
 *  logging in to a {@link SlingRepository}. Can be used to verify 
 *  that default passwords have been disabled on production systems.
 *  Checking for failed logins is the only realistic use case, as
 *  by the credentials will be exposed in plain text in the repository,
 *  which is only ok for default demo passwords of course.   
 */
@Component
@Service(value=RuleBuilder.class)
public class LoginRuleBuilder implements RuleBuilder {

    public static final String NAMESPACE = "sling";
    public static final String RULE_NAME = "login";
    
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
        public Object getValue() {
            String result = "???";
            final Credentials creds = new SimpleCredentials(username, password.toCharArray());
            Session s = null;
            try {
                s = repository.login(creds);
                result = "LOGIN_OK";
            } catch(RepositoryException rex) {
                result = "LOGIN_FAILED";
            } finally {
                if(s != null) {
                    s.logout();
                }
            }
            return result;
        }
        
        @Override
        public String toString() {
            return "Attempt to login as user " + username;
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