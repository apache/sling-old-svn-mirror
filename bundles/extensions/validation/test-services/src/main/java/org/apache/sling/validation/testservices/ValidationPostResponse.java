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
package org.apache.sling.validation.testservices;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationPostResponse extends AbstractPostResponse {

    private ValidationResult validationResult;
    private static final Logger LOG = LoggerFactory.getLogger(ValidationPostResponse.class);

    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    @Override
    protected void doSend(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter printWriter = response.getWriter();
        JSONObject jsonResponse = new JSONObject();
        boolean validationError = false;
        if (validationResult != null) {
            try {
                jsonResponse.put("valid", validationResult.isValid());
                JSONArray failures = new JSONArray();
                for (ValidationFailure failure : validationResult.getFailures()) {
                    JSONObject failureJson = new JSONObject();
                    failureJson.put("message", failure.getMessage());
                    failureJson.put("location", failure.getLocation());
                    failures.put(failureJson);
                }
                jsonResponse.put("failures", failures);
            } catch (JSONException e) {
                LOG.error("JSON error during response send operation.", e);
            }
        } else {
            validationError = true;
        }
        printWriter.write(jsonResponse.toString());
        if (validationError) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    public void onChange(String type, String... arguments) {
        // NOOP
    }
}
