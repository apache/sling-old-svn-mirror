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
package org.apache.sling.sample;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.core.Constants;
import org.apache.sling.core.components.BaseComponent;

/**
 * The <code>Navigation</code> class is a very simple navigation component
 * which just draws a (nested) list of child "pages". A child content is
 * considered a "page" if the Content object is an instance of the
 * {@link SamplePage} class.
 *
 * @scr.component immediate="true" metatype="false"
 * @scr.property name="service.description" value="Sample Navigation Component"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="org.apache.sling.component.Component"
 */
public class Navigation extends BaseComponent {

    @Override
    protected void doInit() {
        // nothing to do
    }

    public void service(ComponentRequest request, ComponentResponse response)
            throws ComponentException, IOException {

        // start the navigation at the location of the current content
        listChildren(request, response, request.getContent());

    }

    private void listChildren(ComponentRequest request,
            ComponentResponse response, Content current)
            throws ComponentException, IOException {

        // / the children of the current content, terminate if there are none
        Enumeration<Content> children = request.getChildren(current);
        if (!children.hasMoreElements()) {
            return;
        }

        // to not draw the link to the content of the current page, we
        // retrieve the path of the page level content
        Content requestContent = (Content) request.getAttribute(Constants.ATTR_REQUEST_CONTENT);
        String requestPath = requestContent.getPath();

        PrintWriter pw = response.getWriter();
        pw.println("<ul>");

        while (children.hasMoreElements()) {
            Content child = children.nextElement();

            // if the child is a page, add an entry with optional link and
            // recursively call this method to draw the children of the child
            if (child instanceof SamplePage) {
                String title = ((SamplePage) child).getTitle();
                pw.print("<li>");

                if (child.getPath().equals(requestPath)) {
                    pw.print(title);
                } else {
                    pw.print("<a href=\"" + child.getPath() + ".html\">");
                    pw.print(title);
                    pw.print("</a>");
                }
                pw.println("</li>");

                // children of the current page, too
                listChildren(request, response, child);
            }
        }

        pw.println("</ul>");
    }
}
