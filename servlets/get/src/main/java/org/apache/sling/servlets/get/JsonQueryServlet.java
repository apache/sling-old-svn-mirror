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
package org.apache.sling.servlets.get;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.servlets.get.helpers.JsonRendererServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SlingSafeMethodsServlet that renders the search results as JSON data
 *
 * @scr.component immediate="true" metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * 
 * @scr.property name="service.description" value="Default Query Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * 
 * Use this as the default query servlet for json get requests for Sling
 * @scr.property name="sling.servlet.resourceTypes"
 *               value="sling/servlet/default"
 * @scr.property name="sling.servlet.extensions" value="json"
 * @scr.property name="sling.servlet.selectors" value="query"
 */
public class JsonQueryServlet extends SlingSafeMethodsServlet {
    private final Logger log = LoggerFactory.getLogger(JsonQueryServlet.class);

    /** Search clause */
    public static final String STATEMENT = "statement";

    /** Query type */
    public static final String QUERY_TYPE = "queryType";

    /** Result set offset */
    public static final String OFFSET = "offset";

    /** Number of rows requested */
    public static final String ROWS = "rows";

    /** property to append to the result */
    public static final String PROPERTY = "property";

    /** exerpt lookup path */
    public static final String EXCERPT_PATH = "excerptPath";

    /** rep:exerpt */
    private static final String REP_EXCERPT = "rep:excerpt()";

    @Override
    protected void doGet(SlingHttpServletRequest req,
            SlingHttpServletResponse resp) throws IOException {
        dumpResult(req, resp);
    }

    /**
     * Dumps the result as JSON object.
     *
     * @param req request
     * @param resp response
     * @throws IOException in case the search will unexpectedly fail
     */
    private void dumpResult(SlingHttpServletRequest req,
            SlingHttpServletResponse resp) throws IOException {
        try {
            ResourceResolver resolver = req.getResourceResolver();

            String statement = req.getParameter(STATEMENT);
            String queryType = (req.getParameter(QUERY_TYPE) != null && req.getParameter(
                QUERY_TYPE).equals(Query.SQL)) ? Query.SQL : Query.XPATH;

            Iterator<Map<String, Object>> result = resolver.queryResources(
                statement, queryType);

            if (req.getParameter(OFFSET) != null) {
                long skip = Long.parseLong(req.getParameter(OFFSET));
                while (skip > 0 && result.hasNext()) {
                    result.next();
                    skip--;
                }
            }

            resp.setContentType(JsonRendererServlet.responseContentType);
            resp.setCharacterEncoding("UTF-8");

            final JSONWriter w = new JSONWriter(resp.getWriter());
            w.array();

            long count = -1;
            if (req.getParameter(ROWS) != null) {
                count = Long.parseLong(req.getParameter(ROWS));
            }

            List<String> properties = new ArrayList<String>();
            if (req.getParameterValues(PROPERTY) != null) {
                for (String property : req.getParameterValues(PROPERTY)) {
                    properties.add(property);
                }
            }

            String exerptPath = "";
            if (req.getParameter(EXCERPT_PATH) != null) {
                exerptPath = req.getParameter(EXCERPT_PATH);
            }

            // iterate through the result set and build the "json result"
            while (result.hasNext() && count != 0) {
                Map<String, Object> row = result.next();

                w.object();
                String path = row.get("jcr:path").toString();

                w.key("name");
                w.value(ResourceUtil.getName(path));

                // dump columns
                for (String colName : row.keySet()) {
                    w.key(colName);
                    String strValue = "";
                    if (colName.equals(REP_EXCERPT)) {
                        Object ev = row.get("rep:excerpt(" + exerptPath + ")");
                        strValue = (ev == null) ? "" : ev.toString();
                    } else {
                        strValue = formatValue(row.get(colName));
                    }
                    w.value(strValue);
                }

                // load properties and add it to the result set
                if (!properties.isEmpty()) {
                    Resource nodeRes = resolver.getResource(path);
                    dumpProperties(w, nodeRes, properties);
                }

                w.endObject();
                count--;
            }
            w.endArray();
        } catch (JSONException je) {
            throw wrapException(je);
        }
    }

    private void dumpProperties(JSONWriter w, Resource nodeRes,
            List<String> properties) throws JSONException {

        // nothing to do if there is no resource
        if (nodeRes == null) {
            return;
        }


        ResourceResolver resolver = nodeRes.getResourceResolver();
        for (String property : properties) {
            Resource prop = resolver.getResource(nodeRes, property);
            if (prop != null) {
                String strValue;
                Value value = prop.adaptTo(Value.class);
                if (value != null) {
                    strValue = formatValue(value);
                } else {
                    strValue = prop.adaptTo(String.class);
                    if (strValue == null) {
                        strValue = "";
                    }
                }
                w.key(property);
                w.value(strValue);
            }
        }

    }

    private String formatValue(Value value) {
        try {
            return formatValue(JcrResourceUtil.toJavaObject(value));
        } catch (RepositoryException re) {
            // might log
        }
        return "";
    }

    private String formatValue(Object value) {
        String strValue;
        if (value instanceof InputStream) {
            // binary value comes as a LazyInputStream
            strValue = "[binary]";

            // just to be clean, close the stream
            try {
                ((InputStream) value).close();
            } catch (IOException ignore) {
            }
        } else if (value != null) {
            strValue = value.toString();
        } else {
            strValue = "";
        }

        return strValue;
    }

    /**
     * @param e
     * @throws org.apache.sling.api.SlingException wrapping the given exception
     */
    private SlingException wrapException(Exception e) {
        log.warn("Error in QueryServlet: " + e.toString(), e);
        return new SlingException(e.toString(), e);
    }
}
