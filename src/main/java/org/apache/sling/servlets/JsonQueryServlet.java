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
package org.apache.sling.servlets;

import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.SlingException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Row;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * A SlingSafeMethodsServlet that renders the search results as JSON data
 *
 * @scr.service
 *  interface="javax.servlet.Servlet"
 *
 * @scr.component
 *  immediate="true"
 *  metatype="false"
 *
 * @scr.property
 *  name="service.description"
 *  value="Default Query Servlet"
 *
 * @scr.property
 *  name="service.vendor"
 *  value="The Apache Software Foundation"
 *
 * Use this as the default query servlet for json get requests for Sling
 * @scr.property
 *  name="sling.servlet.resourceTypes"
 *  value="sling/servlet/default"
 *
 * @scr.property
 *  name="sling.servlet.extensions"
 *  value="json"
 *
 * @scr.property
 *  name="sling.servlet.selectors"
 *  value="query"
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
            Session s = req.getResourceResolver().adaptTo(Session.class);
            if (s == null) {
                throw new IOException("No JCR Session available");
            }
            String statement = req.getParameter(STATEMENT);
            String queryType =
                    (req.getParameter(QUERY_TYPE) != null &&
                            req.getParameter(QUERY_TYPE).equals(Query.SQL)) ?
                            Query.SQL : Query.XPATH;
            QueryManager qm = s.getWorkspace().getQueryManager();
            Query query = qm.createQuery(statement, queryType);
            QueryResult result = query.execute();
            RowIterator rows = result.getRows();
            String cols[] = result.getColumnNames();
            resp.setContentType(JsonRendererServlet.responseContentType);
            final JSONWriter w = new JSONWriter(resp.getWriter());
            w.array();
            if (req.getParameter(OFFSET) != null) {
                long skip = Long.parseLong(req.getParameter(OFFSET));
                rows.skip(skip);
            }
            long count = -1;
            if (req.getParameter(ROWS) != null) {
                count = Long.parseLong(req.getParameter(ROWS));
            }

            List<String> properties = new ArrayList<String>();
            if (req.getParameterValues(PROPERTY) != null) {
                for (String property: req.getParameterValues(PROPERTY)) {
                    properties.add(property);
                }
            }

            String exerptPath = "";
            if (req.getParameter(EXCERPT_PATH) != null) {
                exerptPath = req.getParameter(EXCERPT_PATH);
            }

            // iterate through the result set and build the "json result"
            while (rows.hasNext() && count != 0) {
                Row row = rows.nextRow();
                String path = row.getValue("jcr:path").getString();
                Node node = (Node) s.getItem(path);
                String name = node.getName();

                w.object();
                w.key("name");
                w.value(name);
                for (String colName: cols) {
                    w.key(colName);
                    String strValue = "";
                    if (colName.equals(REP_EXCERPT)) {
                        Value ev = row.getValue("rep:excerpt(" + exerptPath + ")");
                        strValue = ev == null ? "" : ev.getString();
                    } else {
                        Value value = row.getValue(colName);
                        strValue = formatValue(value);
                    }
                    w.value(strValue);
                }

                // load properties and add it to the result set
                for (String property: properties) {
                    if (node.hasProperty(property)) {
                        Value value = node.getProperty(property).getValue();
                        String strValue = formatValue(value);
                        w.key(property);
                        w.value(strValue);
                    }
                }
                w.endObject();
                count--;
            }
            w.endArray();
        } catch (JSONException je) {
            throw wrapException(je);
        } catch (RepositoryException re) {
            throw wrapException(re);
        }
    }

    private String formatValue(Value value) {
        String strValue = "";
        try {
            if (value != null) {
                switch (value.getType()) {
                    case PropertyType.DATE: strValue = value.getDate().toString(); break;
                    case PropertyType.LONG: strValue = String.valueOf(value.getLong());break;
                    case PropertyType.DOUBLE: strValue = String.valueOf(value.getDouble()); break;
                    case PropertyType.BINARY: strValue = "[binary]"; break;
                    default:
                        strValue = value.getString();
                }
            }
        } catch (RepositoryException re) {
            // ignore
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
