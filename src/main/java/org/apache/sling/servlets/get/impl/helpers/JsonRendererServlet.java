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
import org.apache.sling.commons.json.io.JSONRenderer;
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

    /** Selector that means "pretty-print the output */
    public static final String TIDY = "tidy";
    
    /** Selector that causes hierarchy to be rendered as arrays 
     *  instead of child objects - useful to preserve the order of those 
     *  child objects */ 
    public static final String HARRAY = "harray";
    
    /** How much to indent in tidy mode */
    public static final int INDENT_SPACES = 2;

    private long maximumResults;
    
    private final JSONRenderer renderer = new JSONRenderer();

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

        int maxRecursionLevels = 0;
        try {
            maxRecursionLevels = getMaxRecursionLevel(req);
        } catch(IllegalArgumentException iae) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, iae.getMessage());
            return;
        }

        resp.setContentType(req.getResponseContentType());
        resp.setCharacterEncoding("UTF-8");

        // We check the tree to see if the nr of nodes isn't bigger than the allowed nr.
        boolean allowDump = true;
        int allowedLevel = 0;
        final boolean tidy = isTidy(req);
        final boolean harray = hasSelector(req, HARRAY);
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
            // Dump the resource if we can
            if (allowDump) {
                if (tidy || harray) {
                    final JSONRenderer.Options opt = renderer.options()
                            .withIndent(tidy ? INDENT_SPACES : 0)
                            .withArraysForChildren(harray);
                    resp.getWriter().write(renderer.prettyPrint(traversor.getJSONObject(), opt));
                } else {
                    // If no rendering options, use the plain toString() method, for
                    // backwards compatibility. Output might be slightly different
                    // with prettyPrint and no options
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
    
    /**
     * Get recursion level from selectors. as per SLING-167: the last selector, if present, gives the recursion level.
     *
     * @param req the request
     * @return the recursion level
     * @throws IllegalArgumentException if the detected selector is not a number
     */
    protected int getMaxRecursionLevel(SlingHttpServletRequest req) throws IllegalArgumentException {
        int maxRecursionLevels = 0;
        final String[] selectors = req.getRequestPathInfo().getSelectors();
        if (selectors != null && selectors.length > 0) {
            final String level = selectors[selectors.length - 1];
            if(!TIDY.equals(level) && !HARRAY.equals(level)) {
                if (INFINITY.equals(level)) {
                    maxRecursionLevels = -1;
                } else {
                    try {
                        maxRecursionLevels = Integer.parseInt(level);
                    } catch (NumberFormatException nfe) {
                        //SLING-2324
                        if (StringUtils.isNumeric(level)){
                            maxRecursionLevels = -1;
                        } else {
                            throw new IllegalArgumentException("Invalid recursion selector value '" + level + "'"); 
                        }
                    }
                }
            }
        }
        return maxRecursionLevels;
    }

    /**
     * Checks if the provided request contains a certain selector.
     * @param req the request
     * @param selectorToCheck the selector
     * @return {@code true} if the selector is present, {@code false} otherwise
     */
    protected boolean hasSelector(SlingHttpServletRequest req, String selectorToCheck) {
        for(String selector : req.getRequestPathInfo().getSelectors()) {
            if(selectorToCheck.equals(selector)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if our request wants the "tidy" pretty-printed format
     * @param req the request
     * @return {@code true} if the request contains the {@link #TIDY} selector, {@code false} otherwise
     */
    protected boolean isTidy(SlingHttpServletRequest req) {
        return hasSelector(req, TIDY);
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

