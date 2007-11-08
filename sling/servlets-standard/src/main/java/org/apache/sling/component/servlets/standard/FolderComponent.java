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
package org.apache.sling.component.servlets.standard;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.core.components.BaseComponent;

/**
 * The <code>FolderComponent</code> TODO
 *
 * @scr.component immediate="true" metatype="false"
 * @scr.property name="service.description"
 *          value="Component to handle nt:folder content"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service
 */
public class FolderComponent extends BaseComponent {

    public static final String ID = FolderComponent.class.getName();

    {
        this.setContentClassName(FolderContent.class.getName());
        this.setComponentId(ID);
    }

    /**
     * @see org.apache.sling.core.components.BaseComponent#createContentInstance()
     */
    public Content createContentInstance() {
        return new FolderContent();
    }

    // nothing to do
    protected void doInit() {}

    /**
     * @see org.apache.sling.core.component.Component#service(org.apache.sling.core.component.ComponentRequest, org.apache.sling.core.component.ComponentResponse)
     */
    public void service(ComponentRequest request, ComponentResponse response)
            throws IOException {
        FolderContent content = (FolderContent) request.getContent();

        response.setContentType("text/html");
        PrintWriter pw = response.getWriter();
        pw.println("<html><head>");
        pw.println("<title>" + content.getPath() + "</title>");
        pw.println("</head><body bgcolor='white'>");
        pw.println("<h1>Contents of <code>" + content.getPath() + "</code></h1>");
        pw.println("<ul>");

        try {
            Enumeration<Content> entries = request.getChildren(content);
            while (entries.hasMoreElements()) {
                Content entry = entries.nextElement();
                pw.println("<li>" + entry.getPath() + "</li>");
            }
        } catch (ComponentException ce) {
            // TODO: handle
        }

        pw.println("</ul>");
        pw.println("</body></html>");
    }
}
