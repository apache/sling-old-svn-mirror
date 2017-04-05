/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.repoinit.parser.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** A single "set ACL" line */
public class AclLine {
    
    private final Action action;
    private static final List<String> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<String>());
    
    public static final String PROP_PATHS = "paths";
    public static final String PROP_PRINCIPALS = "principals";
    public static final String PROP_PRIVILEGES = "privileges";
    public static final String PROP_NODETYPES = "nodetypes";

    public enum Action {
        REMOVE,
        REMOVE_ALL,
        DENY,
        ALLOW
    };
    
    private final Map<String, List<String>> properties;
    private List<RestrictionClause> restrictions;
    
    public AclLine(Action a) {
        action = a;
        properties = new TreeMap<String, List<String>>();
    }
    
    public Action getAction() {
        return action;
    }
    
    /** Return the named multi-value property, or an empty list
     *  if not found. 
     */
    public List<String> getProperty(String name) {
        List<String> value = properties.get(name);
        return value != null ? value : EMPTY_LIST;
    }
    
    public void setProperty(String name, List<String> values) {
        properties.put(name, Collections.unmodifiableList(values));
    }

    public void setRestrictions(List<RestrictionClause> restrictions){
        this.restrictions = restrictions;
    }

    public List<RestrictionClause> getRestrictions() { return this.restrictions; }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + action + " " + properties + (restrictions == null || restrictions.isEmpty() ? "" : " restrictions="+restrictions);

    }
}
