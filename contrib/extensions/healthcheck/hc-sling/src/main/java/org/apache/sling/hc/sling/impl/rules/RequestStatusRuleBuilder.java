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

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.SystemAttribute;
import org.slf4j.Logger;

/** Creates {@link Rule} to check specified Sling URLs for an
 *  ok (or other) status. Can be supplied with a comma-separated
 *  list of paths (in the rule qualifier) and returns the highest
 *  status in that case.
 */
@Component
@Service(value=RuleBuilder.class)
public class RequestStatusRuleBuilder implements RuleBuilder {

    public static final String NAMESPACE = "sling";
    public static final String RULE_NAME = "request.status";
    
    @Reference
    private SlingRequestProcessor requestProcessor;
    
    @Reference
    private ResourceResolverFactory resolverFactory; 
    
    private class RequestStatusSystemAttribute implements SystemAttribute {

        private final List<String> paths;
        
        RequestStatusSystemAttribute(List<String> paths) {
            this.paths = paths;
        }
        
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName());
            if(paths.size() <= 3) {
                sb.append(", paths=").append(paths);
            } else {
                sb.append(", ").append(paths.size()).append(" paths to check");
            }
            return sb.toString();
        }
        
        @Override
        public Object getValue(Logger logger) {
            ResourceResolver resolver = null;
            int maxStatus = 0;
            int checked = 0;
            
            try {
                // TODO for new all requests are made as admin
                resolver = resolverFactory.getAdministrativeResourceResolver(null);
                for(String path : paths) {
                    final HttpServletRequest request = new InternalRequest(path);
                    final InternalResponse response = new InternalResponse();
                    requestProcessor.processRequest(request, response, resolver);
                    final int status = response.getStatus();
                    logger.debug("{} returns status {}", path, status);
                    checked++;
                    maxStatus = Math.max(maxStatus, status);
                }
            } catch(Exception e) {
                logger.error("Exception while executing request", e);
            } finally {
                if(resolver != null) {
                    resolver.close();
                }
            }
            
            if(checked == 0) {
                logger.error("No paths checked, empty paths list?");
            }
            
            return maxStatus;
        }
    }
    
    @Override
    public Rule buildRule(String namespace, String ruleName, String qualifier, String expression) {
        if(!NAMESPACE.equals(namespace) || !RULE_NAME.equals(ruleName) || qualifier == null) {
            return null;
        }
        
        final List<String> pathList = new LinkedList<String>();
        for(String p : qualifier.split(",")) {
            pathList.add(p.trim());
        }
        
        return new Rule(new RequestStatusSystemAttribute(pathList), expression);
    }
}
