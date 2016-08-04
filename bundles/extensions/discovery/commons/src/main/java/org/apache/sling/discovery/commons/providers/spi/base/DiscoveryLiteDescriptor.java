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

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;

/**
 * Simplifies access to the underlying JSON-backed oak discovery-lite descriptor
 */
public class DiscoveryLiteDescriptor {

    /** TODO: avoid hardcoding the constant here but use an Oak constant class instead if possible */
    public static final String OAK_DISCOVERYLITE_CLUSTERVIEW = "oak.discoverylite.clusterview";

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
            throw new Exception("No Descriptor value available");
        }
        JSONObject descriptor = new JSONObject(descriptorStr);
        return new DiscoveryLiteDescriptor(descriptor);
    }
    
    /** the actual descriptor **/
    private final JSONObject descriptor;

    DiscoveryLiteDescriptor(JSONObject descriptor) {
        this.descriptor = descriptor;
    }
    
    /**
     * Returns the 'me' field of the discovery-lite descriptor
     * @return the 'me' field of the discovery-lite descriptor
     * @throws Exception if anything in the descriptor is wrongly formatted
     */
    public int getMyId() throws Exception {
        Object meObj = descriptor.get("me");
        if (meObj == null || !(meObj instanceof Number)) {
            throw new Exception("getMyId: 'me' value of descriptor not a Number: "+meObj+" (descriptor: "+descriptor+")");
        }
        Number me = (Number)meObj;
        return me.intValue();
    }
    
    private int[] getArray(String name) throws Exception {
        Object deactivatingObj = descriptor.get(name);
        if (deactivatingObj==null || !(deactivatingObj instanceof JSONArray)) {
            throw new Exception("getArray: '" + name + "' value of descriptor not an array: "+deactivatingObj+" (descriptor: "+descriptor+")");
        }
        JSONArray deactivating = (JSONArray) deactivatingObj;
        int[] result = new int[deactivating.length()];
        for(int i=0; i<deactivating.length(); i++) {
            Object obj = deactivating.get(i);
            if (obj==null || !(obj instanceof Number)) {
                throw new Exception("getArray: '" + name + "' at "+i+" null or not a number: "+obj+", (descriptor: "+descriptor+")");
            }
            result[i] = ((Number)obj).intValue();
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
        Object idObj = descriptor.get("id");
        if (idObj == null || !(idObj instanceof String)) {
            throw new Exception("getViewId: 'id' value of descriptor not a String: "+idObj+" (descriptor: "+descriptor+")");
        }
        return String.valueOf(idObj);
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
        return descriptor.toString();
    }

    public Long getSeqNum() throws Exception {
        Object seqObj = descriptor.get("seq");
        if (seqObj == null || !(seqObj instanceof Number)) {
            throw new Exception("getSeqNum: 'seq' value of descriptor not a Number: "+seqObj+" (descriptor: "+descriptor+")");
        }
        Number seqNum = (Number)seqObj;
        return seqNum.longValue();
    }

    public boolean isFinal() throws Exception {
        Object finalObj = descriptor.get("final");
        if (finalObj == null || !(finalObj instanceof Boolean)) {
            throw new Exception("isFinal: 'final' value of descriptor not a Boolean: "+finalObj+" (descriptor: "+descriptor+")");
        }
        Boolean isFinal = (Boolean)finalObj;
        return isFinal;
    }

}
