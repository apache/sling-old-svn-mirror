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
package org.apache.sling.models.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.models.factory.ExportException;
import org.apache.sling.models.factory.MissingExporterException;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
class ExportServlet extends SlingSafeMethodsServlet {

    private static final Logger logger = LoggerFactory.getLogger(ExportServlet.class);

    private final String exporterName;
    private final String registeredSelector;
    private final ModelFactory modelFactory;
    private final ExportedObjectAccessor accessor;
    private final Map<String, String> baseOptions;

    public ExportServlet(ModelFactory modelFactory, String registeredSelector, String exporterName, ExportedObjectAccessor accessor,
                         Map<String, String> baseOptions) {
        this.modelFactory = modelFactory;
        this.registeredSelector = registeredSelector;
        this.exporterName = exporterName;
        this.accessor = accessor;
        this.baseOptions = baseOptions;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        Map<String, String> options = createOptionMap(request);

        String exported;
        try {
            exported = accessor.getExportedString(request, options, modelFactory, exporterName);
        } catch (ExportException e) {
            logger.error("Could not get serializer requested by model.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } catch (MissingExporterException e) {
            logger.error("Could not get serialize model to JSON.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (exported == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(request.getResponseContentType());
        response.getWriter().write(exported);
    }

    private Map<String, String> createOptionMap(SlingHttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        String[] selectors = request.getRequestPathInfo().getSelectors();
        Map<String, String> result = new HashMap<String, String>(baseOptions.size() + parameterMap.size() + selectors.length - 1);
        result.putAll(baseOptions);
        for (String selector : selectors) {
            if (!selector.equals(registeredSelector)) {
                result.put(selector, "true");
            }
        }

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue().length == 0) {
                result.put(entry.getKey(), Boolean.TRUE.toString());
            } else {
                result.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return result;
    }

    public interface ExportedObjectAccessor {
        String getExportedString(SlingHttpServletRequest request, Map<String, String> options, ModelFactory modelFactory, String exporterName) throws ExportException, MissingExporterException;
    }

    public static final ExportedObjectAccessor RESOURCE = new ExportedObjectAccessor() {
        @Override
        public String getExportedString(SlingHttpServletRequest request, Map<String, String> options, ModelFactory modelFactory, String exporterName) throws ExportException, MissingExporterException {
            return modelFactory.exportModelForResource(request.getResource(), exporterName, String.class, options);
        }
    };

    public static final ExportedObjectAccessor REQUEST = new ExportedObjectAccessor() {
        @Override
        public String getExportedString(SlingHttpServletRequest request, Map<String, String> options, ModelFactory modelFactory, String exporterName) throws ExportException, MissingExporterException {
            return modelFactory.exportModelForRequest(request, exporterName, String.class, options);
        }
    };

}
