/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.scripting.resolver;

import java.util.StringTokenizer;

import org.apache.sling.api.request.RequestDispatcherOptions;

/** Parse RequestDispatcherOptions from a human-readable String */
class RequestDispatcherOptionsParser {
    RequestDispatcherOptions parse(String str) {
        if(str==null || str.length() == 0) {
            return null;
        }
        
        // parse string in the form name:value, name:value
        // with a shortcut for the forceResourceType option: if the 
        // string contains no comma or colon, it's the value of that option
        // (which is the most common use of those options)
        RequestDispatcherOptions result = new RequestDispatcherOptions();
        if(str.indexOf(',') < 0 && str.indexOf(':') < 0) {
            result.put(RequestDispatcherOptions.OPT_FORCE_RESOURCE_TYPE, str.trim());
        } else {
            final StringTokenizer tk = new StringTokenizer(str, ",");
            while(tk.hasMoreTokens()) {
                final String [] entry = tk.nextToken().split(":");
                if(entry.length == 2) {
                    result.put(entry[0].trim(), entry[1].trim());
                }
            }
        }
        
        return result;
    }
    
}
