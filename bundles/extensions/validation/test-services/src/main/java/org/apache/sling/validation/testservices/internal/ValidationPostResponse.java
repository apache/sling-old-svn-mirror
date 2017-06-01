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
package org.apache.sling.validation.testservices.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.utils.json.JSONWriter;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationPostResponse extends AbstractPostResponse {

    private ValidationResult validationResult;
    private final ResourceBundle resourceBundle;
    private static final Logger LOG = LoggerFactory.getLogger(ValidationPostResponse.class);

    public ValidationPostResponse(@Nonnull ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    @Override
    protected void doSend(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter printWriter = response.getWriter();
        JSONWriter writer = new JSONWriter(printWriter);
        writer.object();
        boolean validationError = false;
        if (validationResult != null) {
            try {
                writer.key("valid").value(validationResult.isValid());
                writer.key("failures").array();
                for (ValidationFailure failure : validationResult.getFailures()) {
                    writer.object();
                    writer.key("message").value(failure.getMessage(resourceBundle));
                    writer.key("location").value(failure.getLocation());
                    writer.key("severity").value(failure.getSeverity());
                    writer.endObject();
                }
                writer.endArray();
            } catch (IOException e) {
                LOG.error("JSON error during response send operation.", e);
            }
        } else {
            validationError = true;
        }
        writer.endObject();
        if (validationError) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    public void onChange(String type, String... arguments) {
        // NOOP
    }
}
