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
package org.apache.sling.discovery.commons.providers.spi.base;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObjectBuilder;

// {"seq":8,"final":true,"id":"aae34e9a-b08d-409e-be10-9ff4106e5387","me":4,"active":[4],"deactivating":[],"inactive":[1,2,3]}
public class DiscoveryLiteDescriptorBuilder {
    
    private int seqNum;
    private int me;
    private Integer[] activeIds = new Integer[0];
    private Integer[] inactiveIds = new Integer[0];
    private Integer[] deactivating = new Integer[0];
    private String id;
    private boolean isFinal = false;

    public DiscoveryLiteDescriptorBuilder() {
        // nothing here
    }
    
    @Override
    public String toString() {
        try {
            return asJson();
        } catch (JsonException e) {
            return "A DiscoLite["+e+"]";
        }
    }
    
    public DiscoveryLiteDescriptorBuilder setFinal(boolean isFinal) {
        this.isFinal = isFinal;
        return this;
    }

    public DiscoveryLiteDescriptorBuilder seq(int seqNum) {
        this.seqNum = seqNum;
        return this;
    }

    public DiscoveryLiteDescriptorBuilder me(int me) {
        this.me = me;
        return this;
    }

    public DiscoveryLiteDescriptorBuilder id(String id) {
        this.id = id;
        return this;
    }

    public DiscoveryLiteDescriptorBuilder activeIds(Integer... activeIds) {
        this.activeIds = activeIds;
        return this;
    }

    public DiscoveryLiteDescriptorBuilder inactiveIds(Integer... inactiveIds) {
        this.inactiveIds = inactiveIds;
        return this;
    }

    public DiscoveryLiteDescriptorBuilder deactivatingIds(Integer... deactivating) {
        this.deactivating = deactivating;
        return this;
    }
    
    public String asJson() {
        JsonObjectBuilder json = Json.createObjectBuilder();
        if (id != null)
        {
            json.add("id", id);
        }
        json.add("final", isFinal);
        json.add("me", me);
        json.add("seq", seqNum);
        json.add("active", toArray(Arrays.asList(activeIds)));
        json.add("inactive", toArray(Arrays.asList(inactiveIds)));
        json.add("deactivating", toArray(Arrays.asList(deactivating)));
        StringWriter writer = new StringWriter();
        
        Json.createGenerator(writer).write(json.build()).close();
        
        return writer.toString();
    }
    
    private JsonArray toArray(List<Integer> values) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Integer value : values)
        {
            builder.add(value);
        }
        return builder.build();
    }
    

}
