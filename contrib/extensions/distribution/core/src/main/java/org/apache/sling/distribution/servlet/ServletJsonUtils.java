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

package org.apache.sling.distribution.servlet;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.distribution.DistributionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for writing json data to http responses.
 */
class ServletJsonUtils {

    private final static Logger log = LoggerFactory.getLogger(ServletJsonUtils.class);

    public static void writeJson(SlingHttpServletResponse response, DistributionResponse distributionResponse) throws IOException {
        JSONObject json = new JSONObject();
        try {
            json.put("success", distributionResponse.isSuccessful());
            json.put("state", distributionResponse.getState().name());
            json.put("message", distributionResponse.getMessage());

        } catch (JSONException e) {
            log.error("Cannot write json", e);
        }

        switch (distributionResponse.getState()) {
            case DISTRIBUTED:
                response.setStatus(200);
                break;
            case DROPPED:
                response.setStatus(400);
                break;
            case ACCEPTED:
                response.setStatus(202);
                break;
        }
        response.getWriter().append(json.toString());
    }

    public static void writeJson(SlingHttpServletResponse response, int status, String message) throws IOException {
        JSONObject json = new JSONObject();
        try {
            json.put("message", message);
        } catch (JSONException e) {
            log.error("Cannot write json", e);
        }
        response.setStatus(status);

        response.getWriter().append(json.toString());
    }
}
