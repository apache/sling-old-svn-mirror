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
package org.apache.sling.discovery.commons.providers.spi.impl;

import java.util.Arrays;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

// {"seq":8,"final":true,"id":"aae34e9a-b08d-409e-be10-9ff4106e5387","me":4,"active":[4],"deactivating":[],"inactive":[1,2,3]}
public class DiscoLite {
    
    private int seqNum;
    private int me;
    private Integer[] activeIds = new Integer[0];
    private Integer[] inactiveIds = new Integer[0];
    private Integer[] deactivating = new Integer[0];

    public DiscoLite() {
        // nothing here
    }
    
    public DiscoLite seq(int seqNum) {
        this.seqNum = seqNum;
        return this;
    }

    public DiscoLite me(int me) {
        this.me = me;
        return this;
    }

    public DiscoLite activeIds(Integer... activeIds) {
        this.activeIds = activeIds;
        return this;
    }

    public DiscoLite inactiveIds(Integer... inactiveIds) {
        this.inactiveIds = inactiveIds;
        return this;
    }

    public DiscoLite deactivatingIds(Integer... deactivating) {
        this.deactivating = deactivating;
        return this;
    }
    
    public String asJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("me", me);
        json.put("seq", seqNum);
        json.put("active", new JSONArray(Arrays.asList(activeIds)));
        json.put("inactive", new JSONArray(Arrays.asList(inactiveIds)));
        json.put("deactivating", new JSONArray(Arrays.asList(deactivating)));
        return json.toString();
    }
}
