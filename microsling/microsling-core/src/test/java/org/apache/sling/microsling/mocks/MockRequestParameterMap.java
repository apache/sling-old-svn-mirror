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
package org.apache.sling.microsling.mocks;

import java.util.HashMap;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;

public class MockRequestParameterMap 
    extends HashMap<String, RequestParameter[]> 
    implements RequestParameterMap {

    public RequestParameter[] getValues(String name) {
        return get(name);
    }

    public RequestParameter getValue(String name) {
        RequestParameter[] values = get(name);
        return (values != null && values.length > 0) ? values[0] : null;
    }
    
    public void addValue(String name,RequestParameter value) {
        put(name, new RequestParameter[] { value });
    }
}
