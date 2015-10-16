/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.client.forms;

import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Vals {
    private Map<String, List<NameValuePair>> data = new LinkedHashMap<String, List<NameValuePair>>();

    public void add(String name, NameValuePair pair) {
        if (data.containsKey(name)) {
            data.get(name).add(pair);
        } else {
            ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(pair);
            data.put(name, list);
        }
    }

    public boolean has(String name) {
        return data.containsKey(name);
    }

    public void set(NameValuePair pair) {
        ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();
        list.add(pair);
        data.put(pair.getName(), list);
    }

    public List<? extends NameValuePair> flatten() {
        List<NameValuePair> result = new ArrayList<NameValuePair>();

        for (List<NameValuePair> c : data.values()) {
            result.addAll(c);
        }

        return result;
    }
}
