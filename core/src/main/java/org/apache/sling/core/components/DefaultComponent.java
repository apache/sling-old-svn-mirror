/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.components;

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

import org.apache.commons.collections.BeanMap;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.component.RequestParameter;
import org.apache.sling.content.ContentManager;
import org.apache.sling.core.RequestUtil;
import org.apache.sling.core.components.BaseComponent;


/**
 * The <code>DefaultComponent</code> TODO
 *
 * @scr.component immediate="true"
 * @scr.property name="service.description" value="Default Component"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service
 */
public class DefaultComponent extends BaseComponent {

    public static final String ID = DefaultComponent.class.getName();

    public DefaultComponent() {
        super(ID);
    }

    protected void doInit() {
    }

    public void service(ComponentRequest request, ComponentResponse response)
            throws ComponentException, IOException {

        // if there are parameters, update the content object
        this.updateContent(request);

        // render the content object
        this.renderContent(request, response);
    }

    private void updateContent(ComponentRequest request)
            throws ComponentException {

        Map parameters = request.getRequestParameterMap();
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        Content content = request.getContent();
        Map contentMap = this.asMap(content);

        // special _delete property to remove a property
        RequestParameter[] toRemove = (RequestParameter[]) parameters.get("_delete");
        for (int i=0; toRemove != null && i < toRemove.length; i++) {
            String[] names = toRemove[i].getString().split("[, ]");
            for (int j=0; j < names.length; j++) {
                contentMap.remove(names[j]);
            }
        }

        for (Iterator pi = parameters.entrySet().iterator(); pi.hasNext();) {
            Map.Entry pEntry = (Map.Entry) pi.next();
            String name = (String) pEntry.getKey();
            if ("_delete".equals(name)) {
                continue;
            }

            RequestParameter[] values = (RequestParameter[]) pEntry.getValue();

            try {
                if (values == null || values.length == 0) {
                    contentMap.remove(name);
                } else if (values.length == 1) {
                    contentMap.put(name, this.toObject(values[0]));
                } else {
                    List valueList = new ArrayList();
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
            ContentManager cm = RequestUtil.getContentManager(request);
            if (cm != null) {
                cm.store(content);
                cm.save();
            }
        } catch (Throwable t) {
            throw new ComponentException("Cannot update " + content.getPath(),
                t);
        }
    }

    private void renderContent(ComponentRequest request,
            ComponentResponse response) throws IOException {

        Content content = request.getContent();

        // format response according to extension (use Mime mapping instead)
        String extension = request.getExtension();
        if ("html".equals(extension) || "htm".equals(extension)) {
            this.renderContentHtml(content, response);
        } else if ("xml".equals(extension)) {
            this.renderContentXML(content, response);
        } else if ("properties".equals(extension)) {
            this.renderContentProperties(content, response);
        } else if ("json".equals(extension)) {
            this.renderContentJson(content, response);
        } else {
            // default rendering as plain text
            this.renderContentText(content, response);
        }
    }

    private void renderContentHtml(Content content, ComponentResponse response)
    throws IOException {
        Map contentMap = new TreeMap(this.asMap(content));

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter pw = response.getWriter();

        pw.println("<html><head><title>");
        pw.println(content.getPath());
        pw.println("</title></head><body bgcolor='white' fgcolor='black'>");
        pw.println("<h1>Contents of <code>" + content.getPath()
            + "</code></h1>");

        pw.println("<table>");
        pw.println("<tr><th>name</th><th>Value</th></tr>");
        for (Iterator ei = contentMap.entrySet().iterator(); ei.hasNext();) {
            Map.Entry entry = (Map.Entry) ei.next();

            pw.println("<tr><td>" + entry.getKey() + "</td><td>"
                + entry.getValue() + "</td></tr>");
        }

        pw.println("</body></html>");
    }

    private void renderContentText(Content content, ComponentResponse response)
    throws IOException {
        Map contentMap = new TreeMap(this.asMap(content));

        response.setContentType("text/plain; charset=UTF-8");
        PrintWriter pw = response.getWriter();

        pw.println("Contents of " + content.getPath());
        pw.println();

        for (Iterator ei = contentMap.entrySet().iterator(); ei.hasNext();) {
            Map.Entry entry = (Map.Entry) ei.next();

            pw.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private void renderContentProperties(Content content, ComponentResponse response)
    throws IOException {
        Properties props = new Properties();
        for (Iterator ci=this.asMap(content).entrySet().iterator(); ci.hasNext(); ) {
            Map.Entry cEntry = (Map.Entry) ci.next();
            props.setProperty(String.valueOf(cEntry.getKey()),
                String.valueOf(cEntry.getValue()));
        }

        response.setContentType("text/plain; charset=ISO-8859-1");

        OutputStream out = response.getOutputStream();
        props.store(out, "Contents of " + content.getPath());
    }

    private void renderContentXML(Content content, ComponentResponse response)
    throws IOException {
        Map contentMap = new TreeMap(this.asMap(content));

        response.setContentType("text/xml; charset=UTF-8");
        PrintWriter pw = response.getWriter();

        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.println("<content>");

        for (Iterator ei = contentMap.entrySet().iterator(); ei.hasNext();) {
            Map.Entry entry = (Map.Entry) ei.next();

            pw.println("  <property>");
            pw.println("    <name>" + entry.getKey() + "</name>");

            if (entry.getValue() instanceof Collection) {
                pw.println("    <values>");
                Collection coll = (Collection) entry.getValue();
                for (Iterator ci=coll.iterator(); ci.hasNext(); ) {
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

    private void renderContentJson(Content content, ComponentResponse response)
            throws IOException {
        // {"newValue":"test",
        // "primaryType":"nt:unstructured",
        // "multi":"[eins, zwei]",
        // "path":"/test",
        // "avalue":"a"
        // }

        Map contentMap = new TreeMap(this.asMap(content));

        response.setContentType("text/x-json; charset=UTF-8");
        PrintWriter pw = response.getWriter();

        pw.println("{");

        for (Iterator ei = contentMap.entrySet().iterator(); ei.hasNext();) {
            Map.Entry entry = (Map.Entry) ei.next();

            pw.print("  \"" + entry.getKey() + "\": ");

            if (entry.getValue() instanceof Collection) {
                pw.println("[");
                Collection coll = (Collection) entry.getValue();
                for (Iterator ci=coll.iterator(); ci.hasNext(); ) {
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

            if (ei.hasNext()) {
                pw.println(',');
            }
        }

        pw.println();
        pw.println("}");

    }

    private void printObjectJson(PrintWriter pw, Object object) {
        boolean quote = !((object instanceof Boolean) || (object instanceof Number));
        if (quote) pw.print('"');
        pw.print(object);
        if (quote) pw.print('"');
    }

    private Map asMap(Content content) {
        if (content instanceof Map) {
            return (Map) content;
        }

        return new BeanMap(content);
    }

    private Object toObject(RequestParameter parameter) throws IOException {
        if (parameter.isFormField()) {
            return parameter.getString();
        }

        return parameter.getInputStream();
    }
}
