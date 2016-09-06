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
package org.apache.sling.pipes.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.pipes.DefaultOutputWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * writes current resource, dubbing a given child resource "writer" property/value pairs, allowing expressions
 */
public class CustomWriter extends DefaultOutputWriter {
    public static final String PATH_KEY = "path";

    public static final String PARAM_WRITER = "writer";

    public static final String[] IGNORED_KEYS = {"jcr:primaryType"};

    Map<String, Object> customOutputs;

    @Override
    public boolean handleRequest(SlingHttpServletRequest request) {
        Resource resource = request.getResource().getChild(PARAM_WRITER);
        if (resource != null){
            customOutputs = new HashMap<>();
            customOutputs.putAll(resource.adaptTo(ValueMap.class));
            for (String ignoredKey : IGNORED_KEYS){
                customOutputs.remove(ignoredKey);
            }
            return true;
        }
        return false;
    }

    @Override
    public void writeItem(Resource resource) throws JSONException {
        writer.object();
        writer.key(PATH_KEY).value(resource.getPath());
        for (Map.Entry<String, Object> entry : customOutputs.entrySet()){
            Object o = pipe.getBindings().instantiateObject((String)entry.getValue());
            writer.key(entry.getKey()).value(o);
        }
        writer.endObject();
    }
}