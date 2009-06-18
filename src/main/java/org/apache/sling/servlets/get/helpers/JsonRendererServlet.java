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
package org.apache.sling.servlets.get.helpers;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JsonRendererServlet</code> renders the current resource in JSON
 * on behalf of the {@link org.apache.sling.servlets.get.DefaultGetServlet}.
 */
public class JsonRendererServlet extends SlingSafeMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(JsonRendererServlet.class);

    private static final long serialVersionUID = 5577121546674133317L;

    public static final String EXT_JSON = "json";
    
    public static final String responseContentType = "application/json";

    private final JsonItemWriter itemWriter;

    /** Recursion level selector that means "all levels" */
    public static final String INFINITY = "infinity";

    public JsonRendererServlet() {
        itemWriter = new JsonItemWriter(null);
    }

    @Override
    protected void doGet(SlingHttpServletRequest req,
            SlingHttpServletResponse resp) throws IOException {
        // Access and check our data
        final Resource r = req.getResource();
        if (r instanceof NonExistingResource) {
            throw new ResourceNotFoundException("No data to dump");
        }

        // Do we have a Property?
        final Property p = r.adaptTo(Property.class);
        if (p != null) {
            try {
                renderProperty(p, resp);
            } catch (JSONException je) {
                reportException(je);
            } catch (RepositoryException re) {
                reportException(re);
            }
            return;
        }

        // Send empty response if we don't have a Node
        final Node n = r.adaptTo(Node.class);
        if (n == null) {
            resp.setContentType(responseContentType);
            resp.setCharacterEncoding("UTF-8");
            return;
        }

        // SLING-167: the last selector, if present, gives the number of
        // recursion levels, 0 being the default
        int maxRecursionLevels = 0;
        final String[] selectors = req.getRequestPathInfo().getSelectors();
        if (selectors != null && selectors.length > 0) {
            String level = selectors[selectors.length - 1];
            if (INFINITY.equals(level)) {
                maxRecursionLevels = -1;
            } else {
                try {
                    maxRecursionLevels = Integer.parseInt(level);
                } catch (NumberFormatException nfe) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid recursion selector value '" + level + "'");
                    return;
                }
            }
        }

        resp.setContentType(responseContentType);
        resp.setCharacterEncoding("UTF-8");

        // do the dump
        try {
            itemWriter.dump(n, resp.getWriter(), maxRecursionLevels);
        } catch (JSONException je) {
            reportException(je);
        } catch (RepositoryException re) {
            reportException(re);
        }
    }

    /** Render a Property by dumping its String value */
    private void renderProperty(Property p, SlingHttpServletResponse resp)
            throws JSONException, RepositoryException, IOException {
        resp.setContentType(responseContentType);
        resp.setCharacterEncoding("UTF-8");
        new JsonItemWriter(null).dump(p, resp.getWriter());
    }

    /**
     * @param e
     * @throws SlingException wrapping the given exception
     */
    private void reportException(Exception e) {
        log.warn("Error in JsonRendererServlet: " + e.toString(), e);
        throw new SlingException(e.toString(), e);
    }
}
