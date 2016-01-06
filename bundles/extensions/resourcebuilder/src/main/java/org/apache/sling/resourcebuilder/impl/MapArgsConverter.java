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
package org.apache.sling.resourcebuilder.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Convert arguments which are a list of Object to a Map, used
 *  to simplify our builder's syntax.
 */
public class MapArgsConverter {
    
    /** Convert an args list to a Map */
    public static Map<String, Object> toMap(Object ... args) {
        if(args.length % 2 != 0) {
            throw new IllegalArgumentException("args must be an even number of name/values:" + Arrays.asList(args));
        }
        final Map<String, Object> result = new HashMap<String, Object>();
        for(int i=0 ; i < args.length; i+=2) {
            result.put(args[i].toString(), args[i+1]);
        }
        return Collections.unmodifiableMap(result);
    }

}