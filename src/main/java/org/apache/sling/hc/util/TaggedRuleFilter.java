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
package org.apache.sling.hc.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleFilter;

/** {@link RulerFilter} that accepts {@link Rule} that have certain tags */
public class TaggedRuleFilter implements RuleFilter {
    private final List<String> tags = new ArrayList<String>();
    
    /** Create a RuleFilter that selects Rules
     *  having all supplied tags (lowercased)
     */
    public TaggedRuleFilter(String ...inputTags) {
        
        // Trim and ignore empty tags
        for(int i=0 ; i < inputTags.length; i++) {
            String tag = inputTags[i].trim();
            if(tag.length() > 0) {
                tags.add(tag.toLowerCase());
            }
        }
    }
    
    public boolean accept(Rule r) {
        for(String tag : tags) {
            if(!r.hasTag(tag)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ", tags=" + tags;
    }
}
