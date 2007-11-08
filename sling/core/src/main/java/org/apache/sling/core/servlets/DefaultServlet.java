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
package org.apache.sling.core.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.beanutils.BeanMap;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceManager;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.ComponentException;

/**
 * The <code>DefaultServlet</code> TODO
 * <p>
 * The default servlet is not registered to handle any concrete resource
 * type. Rather it is used internally on demand.
 *
 * @scr.component immediate="true"
 * @scr.property name="service.description" value="Default Component"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @ not to be used as scr.service
 */
public class DefaultServlet extends SlingAllMethodsServlet {

    protected void doInit() {
    }

    @Override
    protected void doPost(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        RequestParameterMap parameters = request.getRequestParameterMap();
        if (parameters == null || parameters.isEmpty()) {
            // just redirect to display the resource
            response.sendRedirect(request.getRequestURI());
            return;
        }

        Resource resource = request.getResource();
        Map<Object, Object> contentMap = asMap(resource);

        // special _delete property to remove a property
        RequestParameter[] toRemove = parameters.get("_delete");
        for (int i = 0; toRemove != null && i < toRemove.length; i++) {
            String[] names = toRemove[i].getString().split("[, ]");
            for (int j = 0; j < names.length; j++) {
                contentMap.remove(names[j]);
            }
        }

        for (Iterator<Map.Entry<String, RequestParameter[]>> pi = parameters.entrySet().iterator(); pi.hasNext();) {
            Map.Entry<String, RequestParameter[]> pEntry = pi.next();
            String name = pEntry.getKey();
            if ("_delete".equals(name)) {
                continue;
            }

            RequestParameter[] values = pEntry.getValue();

            try {
                if (values == null || values.length == 0) {
                    contentMap.remove(name);
                } else if (values.length == 1) {
                    contentMap.put(name, this.toObject(values[0]));
                } else {
                    List<Object> valueList = new ArrayList<Object>();
                    for (int i = 0; i < values.length; i++) {
                        valueList.add(this.toObject(values[i]));
                    }
                    contentMap.put(name, valueList);
                }
            } catch (Throwable t) {
                // should actually handle
            }
        }

        try {
            ResourceManager rm = (ResourceManager) request.getResourceResolver();
            if (rm != null) {
                rm.store(resource);
                rm.save();
            }
        } catch (Throwable t) {
            throw new ComponentException("Cannot update " + resource.getURI(),
                t);
        }

        // have the resource rendered now
        response.sendRedirect(request.getRequestURI());
    }

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        Resource resource = request.getResource();

        // format response according to extension (use Mime mapping instead)
        String extension = request.getRequestPathInfo().getExtension();
        if ("html".equals(extension) || "htm".equals(extension)) {
            this.renderContentHtml(resource, response);
        } else if ("xml".equals(extension)) {
            this.renderContentXML(resource, response);
        } else if ("properties".equals(extension)) {
            this.renderContentProperties(resource, response);
        } else if ("json".equals(extension)) {
            this.renderContentJson(resource, response);
        } else {
            // default rendering as plain text
            this.renderContentText(resource, response);
        }
    }

    private void renderContentHtml(Resource resource,
            SlingHttpServletResponse response) throws IOException {
        Map<Object, Object> contentMap = new TreeMap<Object, Object>(
            this.asMap(resource));

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter pw = response.getWriter();

        pw.println("<html><head><title>");
        pw.println(resource.getURI());
        pw.println("</title></head><body bgcolor='white' fgcolor='black'>");
        pw.println("<h1>Contents of <code>" + resource.getURI()
            + "</code></h1>");

        pw.println("<table>");
        pw.println("<tr><th>name</th><th>Value</th></tr>");

        for (Map.Entry<Object, Object> entry : contentMap.entrySet()) {
            pw.println("<tr><td>" + entry.getKey() + "</td><td>"
                + entry.getValue() + "</td></tr>");
        }

        pw.println("</body></html>");
    }

    private void renderContentText(Resource resource,
            SlingHttpServletResponse response) throws IOException {
        Map<Object, Object> contentMap = new TreeMap<Object, Object>(
            this.asMap(resource));

        response.setContentType("text/plain; charset=UTF-8");
        PrintWriter pw = response.getWriter();

        pw.println("Contents of " + resource.getURI());
        pw.println();

        for (Map.Entry<Object, Object> entry : contentMap.entrySet()) {
            pw.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private void renderContentProperties(Resource resource,
            SlingHttpServletResponse response) throws IOException {
        Properties props = new Properties();

        Map<Object, Object> contentMap = new TreeMap<Object, Object>(
            this.asMap(resource));
        for (Map.Entry<Object, Object> entry : contentMap.entrySet()) {
            props.setProperty(String.valueOf(entry.getKey()),
                String.valueOf(entry.getValue()));
        }

        response.setContentType("text/plain; charset=ISO-8859-1");

        OutputStream out = response.getOutputStream();
        props.store(out, "Contents of " + resource.getURI());
    }

    private void renderContentXML(Resource resource,
            SlingHttpServletResponse response) throws IOException {
        Map<Object, Object> contentMap = new TreeMap<Object, Object>(
            this.asMap(resource));

        response.setContentType("text/xml; charset=UTF-8");
        PrintWriter pw = response.getWriter();

        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<content>");

        for (Map.Entry<Object, Object> entry : contentMap.entrySet()) {

            pw.println("  <property>");
            pw.println("    <name>" + entry.getKey() + "</name>");

            if (entry.getValue() instanceof Collection) {
                pw.println("    <values>");
                Collection<?> coll = (Collection<?>) entry.getValue();
                for (Iterator<?> ci = coll.iterator(); ci.hasNext();) {
                    pw.println("      <value>" + ci.next() + "</value>");
                }
                pw.println("    </values>");

            } else {
                pw.println("    <value>" + entry.getValue() + "</value>");
            }
            pw.println("  </property>");
        }

        pw.println("</content>");
    }

    private void renderContentJson(Resource resource,
            SlingHttpServletResponse response) throws IOException {
        // {"newValue":"test",
        // "primaryType":"nt:unstructured",
        // "multi":"[eins, zwei]",
        // "path":"/test",
        // "avalue":"a"
        // }

        Map<Object, Object> contentMap = new TreeMap<Object, Object>(
            this.asMap(resource));

        response.setContentType("text/x-json; charset=UTF-8");
        PrintWriter pw = response.getWriter();

        pw.println("{");

        boolean notFirst = false;
        for (Map.Entry<Object, Object> entry : contentMap.entrySet()) {

            if (notFirst) {
                pw.println(',');
            } else {
                notFirst = true;
            }

            pw.print("  \"" + entry.getKey() + "\": ");

            if (entry.getValue() instanceof Collection) {
                pw.println("[");
                Collection<?> coll = (Collection<?>) entry.getValue();
                for (Iterator<?> ci = coll.iterator(); ci.hasNext();) {
                    pw.print("    ");
                    this.printObjectJson(pw, ci.next());
                    if (ci.hasNext()) {
                        pw.println(',');
                    }
                }
                pw.println();
                pw.print("  ]");

            } else {
                this.printObjectJson(pw, entry.getValue());
            }
        }

        pw.println();
        pw.println("}");

    }

    private void printObjectJson(PrintWriter pw, Object object) {
        boolean quote = !((object instanceof Boolean) || (object instanceof Number));
        if (quote) {
            pw.print('"');
        }
        pw.print(object);
        if (quote) {
            pw.print('"');
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> asMap(Resource resource) {
        Object object = resource.getObject();
        if (object instanceof Map) {
            return (Map<Object, Object>) object; // unchecked cast
        }

        return new BeanMap(object); // unchecked cast
    }

    private Object toObject(RequestParameter parameter) throws IOException {
        if (parameter.isFormField()) {
            return parameter.getString();
        }

        return parameter.getInputStream();
    }
}
