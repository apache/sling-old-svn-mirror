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

package org.apache.sling.acldef.parser.operations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AclLine {
    
    private final Action action;
    
    public enum Action {
        REMOVE,
        REMOVE_ALL,
        DENY,
        ALLOW
    };
    
    private final List<String> privileges;
    private final List<String> usernames;
    
    public AclLine(Action a, List<String> privileges, List<String> usernames) {
        action = a;
        this.usernames = Collections.unmodifiableList(usernames);
        this.privileges = privileges == null ? null : Collections.unmodifiableList(privileges);
    }
    
    public Action getAction() {
        return action;
    }
    
    public Collection<String> getUsernames() {
        return usernames;
    }
    
    @Override
    public String toString() {
        return action + " " + privileges + " for " + usernames;
    }
}
