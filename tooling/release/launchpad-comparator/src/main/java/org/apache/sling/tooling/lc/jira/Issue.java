/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.tooling.lc.jira;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.internal.util.Objects;

public class Issue implements Comparable<Issue> {
    
    private static final Pattern KEY_PATTERN = Pattern.compile("^([A-Z]+)-(\\d+)$"); 

    private final String key;
    private final Fields fields;

    public Issue(String key, Fields fields) {
        this.key = key;
        this.fields = fields;
    }

    public String getKey() {
        return key;
    }
    
    public Fields getFields() {
        return fields;
    }
    
    public String getSummary() {
        return fields.getSummary();
    }
    
    @Override
    public String toString() {
        return key + " - " + getSummary();
    }

    @Override
    public int compareTo(Issue o) {
        
        Matcher ourMatcher = KEY_PATTERN.matcher(key);
        Matcher theirMatcher = KEY_PATTERN.matcher(o.key);
        
        if ( !ourMatcher.matches()) {
            throw new IllegalArgumentException("No match found for " + key);
        }

        if ( !theirMatcher.matches()) {
            throw new IllegalArgumentException("No match found for " + o.key);
        }
        
        String ourProject = ourMatcher.group(1);
        String theirProject = theirMatcher.group(1);
        
        if ( !Objects.equal(ourProject, theirProject)) {
            return ourProject.compareTo(theirProject);
        }
        
        int ourId = Integer.parseInt(ourMatcher.group(2));
        int theirId = Integer.parseInt(theirMatcher.group(2));

        return Integer.valueOf(ourId).compareTo(theirId);
    }
}
