/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.examples.servlets;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.felix.utils.json.JSONWriter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.model.ValidationModel;

@SlingServlet(
        resourceTypes = "/apps/validationdemo/components/user",
        selectors = {"modify"},
        methods = "POST"
)
public class ModifyUserServlet extends SlingAllMethodsServlet {

    @Reference
    private ValidationService validationService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        ValueMap requestParameters = request.adaptTo(ValueMap.class);
        String[] resourceTypeValues = requestParameters.get("sling:resourceType", String[].class);
        String resourceType = null;
        if (resourceTypeValues != null && resourceTypeValues.length > 0) {
            resourceType = resourceTypeValues[0];
        }
        if (resourceType != null && !"".equals(resourceType)) {
            String resourcePath = request.getRequestPathInfo().getResourcePath();
            ValidationModel vm = validationService.getValidationModel(resourceType, resourcePath, false);
            if (vm != null) {
                ValidationResult vr = validationService.validate(requestParameters, vm);
                if (vr.isValid()) {
                    RequestDispatcherOptions options = new RequestDispatcherOptions();
                    options.setReplaceSelectors(" ");
                    request.getRequestDispatcher(request.getResource(), options).forward(request, response);
                } else {
                    response.setContentType("application/json");
                    JSONWriter writer = new JSONWriter(response.getWriter());
                    writer.object();
                    writer.key("success").value(false);
                    writer.key("failures").array();
                    for (ValidationFailure failure : vr.getFailures()) {
                        writer.object();
                        writer.key("message").value(failure.getMessage(request.getResourceBundle(Locale.US)));
                        writer.key("location").value(failure.getLocation());
                        writer.endObject();
                    }
                    writer.endArray();
                    writer.endObject();
                    response.setStatus(400);
                }
            }
        }
    }
}
