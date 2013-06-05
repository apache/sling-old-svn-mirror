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
package org.apache.sling.hc.sling.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.hc.api.HealthCheckFacade;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.sling.api.RulesResourceParser;

/** Parses a Resource into a list of Rule. See unit tests for details.
 *  TODO should probably be an Adapter instead */
@Component
@Service(value=RulesResourceParser.class)
public class RulesResourceParserImpl implements RulesResourceParser {
    
    @Reference
    private HealthCheckFacade healthcheck;
    
    @Reference
    private SlingRequestProcessor requestProcessor;
    
    public static final String RESOURCE_PATH_INFO = "sling.resource.path";
    
    @SuppressWarnings("serial")
    public static class UnauthorizedException extends SlingException {
        UnauthorizedException() {
            super("Current user is not authorized to execute health check Rules");
        }
    };
    
    @Override
    public List<Rule> parseResource(Resource r) {
        
        try {
            new RulesExecutionPermission().checkPermission(r);
        } catch(RepositoryException rex) {
            throw new UnauthorizedException();
        }
        
        final List<Rule> result = new ArrayList<Rule>();
        recursivelyParseResource(result, r);
        return result;
    }
    
    private void recursivelyParseResource(List<Rule> list, Resource r) {

        // Add Rule for r if available
        final Rule rule = resourceToRule(r);
        if(rule != null) {
            list.add(rule);
        }
        
        // And recurse into r's children
        final Iterator<Resource> it = r.getResourceResolver().listChildren(r);
        while(it.hasNext()) {
            recursivelyParseResource(list, it.next());
        }
    }
    
    /** Convert r to a Rule if possible */
    Rule resourceToRule(Resource r) {
        // If r adapts to a Sling script, use it to evaluate our Rule
        final SlingScript script = r.adaptTo(SlingScript.class);
        if(script != null) {
            return new Rule(new ScriptSystemAttribute(requestProcessor, script), ScriptSystemAttribute.SUCCESS_STRING);
        }

        // else convert using available RuleBuilders if suitable
        final ValueMap props = r.adaptTo(ValueMap.class);
        if(props != null && props.containsKey(NAMESPACE) && props.containsKey(RULE_NAME)) {
            for(RuleBuilder b : healthcheck.getRuleBuilders()) {
                // basic properties
                final Rule rule = b.buildRule(
                    props.get(NAMESPACE, String.class), 
                    props.get(RULE_NAME, String.class), 
                    props.get(QUALIFIER, String.class), 
                    props.get(EXPRESSION, String.class)
                );
                
                // tags, if any
                if(rule != null && props.containsKey(TAGS)) {
                    final String [] tags = props.get(TAGS, String[].class);
                    if(tags != null && tags.length > 0) {
                        rule.setTags(tags);
                    }
                }
                
                // resource path info
                if(rule != null) {
                    rule.getInfo().put(RESOURCE_PATH_INFO, r.getPath());
                }
                
                if(rule != null) {
                    return rule;
                }
            }
        }
        
        return null;
    }
}
