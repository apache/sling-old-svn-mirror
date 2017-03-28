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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReaderFactory;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * Simplifies access to the underlying JSON-backed oak discovery-lite descriptor
 */
public class DiscoveryLiteDescriptor {

    /** TODO: avoid hardcoding the constant here but use an Oak constant class instead if possible */
    public static final String OAK_DISCOVERYLITE_CLUSTERVIEW = "oak.discoverylite.clusterview";

    private static final JsonReaderFactory jsonReaderFactory;
    static {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("org.apache.johnzon.supports-comments", true);
        jsonReaderFactory = Json.createReaderFactory(config);
    }
    /**
     * {"seq":8,"final":true,"id":"aae34e9a-b08d-409e-be10-9ff4106e5387","me":4,"active":[4],"deactivating":[],"inactive":[1,2,3]}
     */
    public static DiscoveryLiteDescriptor getDescriptorFrom(ResourceResolver resourceResolver) throws Exception {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            throw new Exception("Could not adapt resourceResolver to session: "+resourceResolver);
        }
        String descriptorStr = session.getRepository().getDescriptor(DiscoveryLiteDescriptor.OAK_DISCOVERYLITE_CLUSTERVIEW);
        if (descriptorStr == null) {
            throw new Exception("No value available for descriptor " + OAK_DISCOVERYLITE_CLUSTERVIEW);
        }
        JsonObject descriptor = jsonReaderFactory.createReader(new StringReader(descriptorStr)).readObject();
        return new DiscoveryLiteDescriptor(descriptor);
    }
    
    /** the actual descriptor **/
    private final JsonObject descriptor;

    DiscoveryLiteDescriptor(JsonObject descriptor) {
        this.descriptor = descriptor;
    }
    
    /**
     * Returns the 'me' field of the discovery-lite descriptor
     * @return the 'me' field of the discovery-lite descriptor
     * @throws Exception if anything in the descriptor is wrongly formatted
     */
    public int getMyId() throws Exception {
        return descriptor.getInt("me");
    }
    
    private int[] getArray(String name) throws Exception {
        JsonArray deactivating = descriptor.getJsonArray(name);
        
        int[] result = new int[deactivating.size()];
        for(int i=0; i<deactivating.size(); i++) {
            result[i] = deactivating.getInt(i);
        }
        return result;
    }

    /**
     * Returns the 'deactivating' field of the discovery-lite descriptor
     * @return the 'deactivating' field of the discovery-lite descriptor
     * @throws Exception if anything in the descriptor is wrongly formatted
     */
    public int[] getDeactivatingIds() throws Exception {
        return getArray("deactivating");
    }

    /**
     * Returns the 'active' field of the discovery-lite descriptor
     * @return the 'active' field of the discovery-lite descriptor
     * @throws Exception if anything in the descriptor is wrongly formatted
     */
    public int[] getActiveIds() throws Exception {
        return getArray("active");
    }

    /**
     * Returns the 'id' field of the discovery-lite descriptor
     * @return the 'id' field of the discovery-lite descriptor
     * @throws Exception if anything in the descriptor is wrongly formatted
     */
    public String getViewId() throws Exception {
        if (descriptor.isNull("id")) {
            // SLING-5458 : id can now be null,
            // so treat this separately and return null here too
            return null;
        }
        return descriptor.getString("id");
    }

    @Override
    public String toString() {
        return getDescriptorStr();
    }
    
    /**
     * Returns the raw toString of the underlying descriptor
     * @return the raw toString of the underlying descriptor
     */
    public String getDescriptorStr() {
        StringWriter writer = new StringWriter();
        Json.createGenerator(writer).write(descriptor).close();
        return writer.toString();
    }

    public Long getSeqNum() throws Exception {
        return descriptor.getJsonNumber("seq").longValue();
    }

    public boolean isFinal() throws Exception {
        return descriptor.getBoolean("final");
    }

}
