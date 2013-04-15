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
package org.apache.sling.muppet.sling.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.muppet.api.MuppetFacade;
import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.api.RuleBuilder;
import org.apache.sling.muppet.sling.api.RulesResourceParser;

/** Parses a Resource into a list of Rule. See unit tests for details */
@Component
@Service(value=RulesResourceParser.class)
public class RulesResourceParserImpl implements RulesResourceParser {
    
    @Reference
    private MuppetFacade muppet;
    
    @Override
    public List<Rule> parseResource(Resource r) {
        final List<Rule> result = new ArrayList<Rule>();
        recursivelyParseResource(result, r);
        return result;
    }
    
    private void recursivelyParseResource(List<Rule> list, Resource r) {
        final ValueMap props = r.adaptTo(ValueMap.class);
        if(props.containsKey(NAMESPACE) && props.containsKey(RULE_NAME)) {
            for(RuleBuilder b : muppet.getRuleBuilders()) {
                final Rule rule = b.buildRule(
                    props.get(NAMESPACE, String.class), 
                    props.get(RULE_NAME, String.class), 
                    props.get(QUALIFIER, String.class), 
                    props.get(EXPRESSION, String.class)
                );
                if(rule != null) {
                    list.add(rule);
                }
            }
        }
        
        final Iterator<Resource> it = r.getResourceResolver().listChildren(r);
        while(it.hasNext()) {
            recursivelyParseResource(list, it.next());
        }
        
    }
}
