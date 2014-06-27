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
package org.apache.sling.junit;

import java.util.HashMap;
import java.util.Map;

/** Provide test parameters (the "input map") and allow 
 *  tests to provide additional metadata (in an "output
 *  map") about their results.
 *  
 *  Meant to be used to implement performance tests
 *  that run inside Sling instances - we'll expand
 *  the junit.core module to optionally use this.  
 */
public class SlingTestContext {
    private final Map<String, Object> inputMap = new HashMap<String, Object>();
    private final Map<String, Object> outputMap = new HashMap<String, Object>();
    
    public Map<String, Object> input() {
        return inputMap;
    }
    
    public Map<String, Object> output() {
        return outputMap;
    }
}
