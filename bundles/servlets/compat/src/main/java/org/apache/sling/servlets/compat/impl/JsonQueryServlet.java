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
package org.apache.sling.servlets.compat.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.servlets.get.impl.helpers.JsonResourceWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SlingSafeMethodsServlet that renders the search results as JSON data
 *
 * Use this as the default query servlet for json get requests for Sling
 */
@Component
@Service(value=javax.servlet.Servlet.class)
@Properties({
    @Property(name="service.description", value="Default Query Servlet"),
    @Property(name="service.vendor",value="The Apache Software Foundation"),
    @Property(name="sling.servlet.resourceTypes", value="sling/servlet/default"),
    @Property(name="sling.servlet.extensions", value="json"),
    @Property(name="sling.servlet.selectors", value="query"),
    @Property(name="sling.servlet.prefix", intValue=-1)
})
public class JsonQueryServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

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

    public static final String TIDY = "tidy";

    private final JsonResourceWriter itemWriter;

    public JsonQueryServlet() {
        itemWriter = new JsonResourceWriter(null);
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

    @Override
    protected void doGet(SlingHttpServletRequest req,
            SlingHttpServletResponse resp) throws IOException {
        dumpResult(req, resp);
    }

    /**
     * Retrieve the query type from the request.
     *
     * @param req request
     * @return the query type.
     *
     */
    protected String getQueryType(SlingHttpServletRequest req) {
        return req.getParameter(QUERY_TYPE);
    }


    /**
     * Retrieve the query statement from the request.
     *
     * @param req request
     * @param queryType the query type, as previously determined
     * @return the query statement.
     *
     */
    protected String getStatement(SlingHttpServletRequest req, String queryType) {
        return req.getParameter(STATEMENT);
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

            String queryType = getQueryType(req);

            String statement = getStatement(req, queryType);

            Iterator<Map<String, Object>> result = resolver.queryResources(
                statement, queryType);


            if (req.getParameter(OFFSET) != null) {
                long skip = Long.parseLong(req.getParameter(OFFSET));
                while (skip > 0 && result.hasNext()) {
                    result.next();
                    skip--;
                }
            }

            resp.setContentType(req.getResponseContentType());
            resp.setCharacterEncoding("UTF-8");

            final JSONWriter w = new JSONWriter(resp.getWriter());
            w.setTidy(isTidy(req));

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
                        w.value(strValue);

                    } else {
                        //strValue = formatValue(row.get(colName));
                    	itemWriter.dumpValue(w, row.get(colName));
                    }
                    //w.value(strValue);
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

        itemWriter.dumpProperties(nodeRes, w, properties);

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
