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
package org.apache.sling.muppet.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.api.RuleBuilder;

public class TextRulesParser {
    private final List<RuleBuilder> builders = new ArrayList<RuleBuilder>();
    
    public void addBuilder(RuleBuilder b) {
        builders.add(b);
    }
    
    public List<Rule> parse(Reader input) throws IOException {
        final List<Rule> result = new ArrayList<Rule>();
        final BufferedReader r = new BufferedReader(input);
        String line = null;
        while( (line = r.readLine()) != null) {
            line = line.trim();
            if(line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            
            // parse line in the form
            //  namespace:rule name:qualifier:expression
            // where qualifier is optional
            final String [] parts = line.split(":");
            String namespace = null;
            String name = null;
            String qualifier = null;
            String expression = null;
            
            if(parts.length == 4) {
                namespace = parts[0].trim();
                name = parts[1].trim();
                qualifier = parts[2].trim();
                expression = parts[3].trim();
            } else if(parts.length == 3) {
                namespace = parts[0].trim();
                name = parts[1].trim();
                expression = parts[2].trim();
            } else {
                // TODO for now, ignore parsing errors
            }
            
            if(namespace != null) {
                for(RuleBuilder b : builders) {
                    final Rule toAdd = b.buildRule(namespace, name, qualifier, expression);
                    if(toAdd != null) {
                        // first builder wins
                        result.add(toAdd);
                    }
                }
            }
        }
        return result;
    }
}
