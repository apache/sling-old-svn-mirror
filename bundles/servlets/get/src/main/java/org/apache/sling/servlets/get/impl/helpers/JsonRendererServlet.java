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
package org.apache.sling.servlets.get.impl.helpers;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.json.sling.ResourceTraversor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JsonRendererServlet</code> renders the current resource in JSON
 * on behalf of the {@link org.apache.sling.servlets.get.impl.DefaultGetServlet}.
 */
public class JsonRendererServlet extends SlingSafeMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(JsonRendererServlet.class);

    private static final long serialVersionUID = 5577121546674133317L;

    public static final String EXT_JSON = "json";

    /** Recursion level selector that means "all levels" */
    public static final String INFINITY = "infinity";

    public static final String TIDY = "tidy";

    private long maximumResults;

    public JsonRendererServlet(long maximumResults) {
        this.maximumResults = maximumResults;
    }

    @Override
    protected void doGet(SlingHttpServletRequest req,
            SlingHttpServletResponse resp) throws IOException {
        // Access and check our data
        final Resource r = req.getResource();
        if (ResourceUtil.isNonExistingResource(r)) {
            throw new ResourceNotFoundException("No data to render.");
        }

        // SLING-167: the last selector, if present, gives the number of
        // recursion levels, 0 being the default
        int maxRecursionLevels = 0;
        final String[] selectors = req.getRequestPathInfo().getSelectors();
        if (selectors != null && selectors.length > 0) {
            final String level = selectors[selectors.length - 1];
            if(!TIDY.equals(level)) {
                if (INFINITY.equals(level)) {
                    maxRecursionLevels = -1;
                } else {
                    try {
                        maxRecursionLevels = Integer.parseInt(level);
                    } catch (NumberFormatException nfe) {
                    	//SLING-2324
                    	if (StringUtils.isNumeric(level)){
                    		maxRecursionLevels = -1;
                    	}else{
                    		resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    				"Invalid recursion selector value '" + level + "'");
                    		return;
                    	}
                    }
                }
            }
        }

        resp.setContentType(req.getResponseContentType());
        resp.setCharacterEncoding("UTF-8");

        // We check the tree to see if the nr of nodes isn't bigger than the allowed nr.
        boolean allowDump = true;
        int allowedLevel = 0;
        final boolean tidy = isTidy(req);
        ResourceTraversor traversor = null;
        try {
            traversor = new ResourceTraversor(maxRecursionLevels, maximumResults, r, tidy);
            allowedLevel = traversor.collectResources();
            if ( allowedLevel != -1 ) {
			    allowDump = false;
            }
        } catch (final JSONException e) {
            reportException(e);
        }
        try {
            // Check if we can dump the resource.
            if (allowDump) {
                if (tidy) {
                    resp.getWriter().write(traversor.getJSONObject().toString(2));
                } else {
                    resp.getWriter().write(traversor.getJSONObject().toString());
                }

            } else {
                // We are not allowed to do the dump.
                // Send a 300
                String tidyUrl = (tidy) ? "tidy." : "";
                resp.setStatus(HttpServletResponse.SC_MULTIPLE_CHOICES);
                JSONWriter writer = new JSONWriter(resp.getWriter());
                writer.array();
                while (allowedLevel >= 0) {
                    writer.value(r.getResourceMetadata().getResolutionPath() + "." + tidyUrl + allowedLevel + ".json");
                    allowedLevel--;
                }
                writer.endArray();
            }
        } catch (JSONException je) {
            reportException(je);
        }
    }

    /** True if our request wants the "tidy" pretty-printed format */
    protected boolean isTidy(SlingHttpServletRequest req) {
        for(String selector : req.getRequestPathInfo().getSelectors()) {
            if(TIDY.equals(selector)) {
                return true;
            }
        }
        return false;
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

