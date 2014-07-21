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
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONObject;
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
    public static final int TIDY_INDENTION = 2;

    public static final String ARRAY = "array";

    public static final String CHILDREN_KEY = "_children";
    public static final String CHILD_NAME_KEY = "_name";

    private long maximumResults;

    public JsonRendererServlet(long maximumResults) {
        this.maximumResults = maximumResults;
    }

    private void printIndent(StringBuffer sb, int indent, boolean tidy) {
        if (indent > 0 && tidy) {
            sb.append("\n");
            for (int i = 0; i < indent; i++) {
                sb.append(" ");
            }
        }
    }

    private void printKeyValue(StringBuffer sb, String key, String val, boolean tidy) {
        sb.append(key);
        sb.append(":" + (tidy ? " " : ""));
        sb.append(val);
    }

    private String nestedOrderedJson(JSONObject jsonNode, boolean tidy) {
        return nestedOrderedJson(jsonNode, null, 1, tidy);
    }

    private String nestedOrderedJson(JSONObject jsonNode, String nodeName, int level, boolean tidy) {
        try {
            Iterator<String> keys = jsonNode.keys();
            StringBuffer sb = new StringBuffer("{");
            Map<String, JSONObject> children = new LinkedHashMap<String, JSONObject>();
            int curIndent = tidy ? level * TIDY_INDENTION : 0;

            if (nodeName != null) {
                printIndent(sb, curIndent, tidy);
                printKeyValue(sb, jsonNode.quote(CHILD_NAME_KEY), jsonNode.quote(nodeName), tidy);

                if (keys.hasNext()) {
                    sb.append(",");
                }
            }

            while (keys.hasNext()) {
                String o = keys.next();
                Object v = jsonNode.opt(o);

                if (v instanceof JSONObject) { // child node
                    children.put(o, (JSONObject)v);
                } else {
                    printIndent(sb, curIndent, tidy);

                    printKeyValue(sb, jsonNode.quote(o), tidy ? jsonNode.valueToString(v, TIDY_INDENTION, curIndent) : jsonNode.valueToString(v), tidy);

                    if (keys.hasNext()) {
                        sb.append(',');
                    }
                }
            }

            if (!children.isEmpty()) {
                Iterator childrenIterator = children.entrySet().iterator();

                printIndent(sb, curIndent, tidy);
                printKeyValue(sb, jsonNode.quote(CHILDREN_KEY), "[", tidy);

                while (childrenIterator.hasNext()) {
                    Map.Entry<String, JSONObject> entry = (Map.Entry) childrenIterator.next();
                    String name = entry.getKey();
                    JSONObject child = entry.getValue();

                    printIndent(sb, curIndent + TIDY_INDENTION, tidy);
                    sb.append(nestedOrderedJson(child, name, level + 2, tidy));

                    if (childrenIterator.hasNext()) {
                        sb.append(',');
                    }
                }
                printIndent(sb, curIndent, tidy);
                sb.append(']');
            }

            curIndent -= TIDY_INDENTION;

            if (curIndent == 0) { // handle final closing curly bracket
                sb.append("\n");
            } else {
                printIndent(sb, curIndent, tidy);
            }
            sb.append('}');

            return sb.toString();
        } catch (Exception e) {
            return null;
        }
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
            if(!TIDY.equals(level) && !ARRAY.equals(level)) {
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
        final boolean array = isArray(req);
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
                JSONObject jsonNode = traversor.getJSONObject();

                if (array) {
                    resp.getWriter().write(nestedOrderedJson(jsonNode, tidy));
                } else {
                    String jsonNodeString = tidy ? jsonNode.toString(TIDY_INDENTION) : jsonNode.toString();
                    resp.getWriter().write(jsonNodeString);
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

    /** True if our request wants the children as an ordered array rather than as sub-objects */
    protected boolean isArray(SlingHttpServletRequest req) {
        for(String selector : req.getRequestPathInfo().getSelectors()) {
            if(ARRAY.equals(selector)) {
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

