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
package org.apache.sling.configresource.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * The <code>HtmlServlet</code> lists the files and folders of a folder
 * mapped into the Sling resource tree. The listing produced is similar to the
 * default index listing produced by Apache httpd.
 *
 * @scr.component metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="ConfigAdmin Folder Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.resourceTypes"
 *               refValues.1="ConfigAdminProviderConstants.RESOURCE_TYPE_ROOT"
 *               refValues.2="ConfigAdminProviderConstants.RESOURCE_TYPE_CONFIGURATION_ROOT"
 *               refValues.3="ConfigAdminProviderConstants.RESOURCE_TYPE_FACTORIES_ROOT"
 *               refValues.4="ConfigAdminProviderConstants.RESOURCE_TYPE_CONFIGURATION"
 *               refValues.5="ConfigAdminProviderConstants.RESOURCE_TYPE_FACTORY"
 */
public class HtmlServlet extends SlingSafeMethodsServlet {

    // a number of blanks used to format the listing of folder entries
    private static final String NAME_BLANKS = "                                  ";

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        final Resource res = request.getResource();

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw = response.getWriter();

        pw.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\"> <html>");

        pw.printf("<head><title>Index of %s</title></head><body>%n",
            res.getPath());
        pw.printf("<h1>Index of %s</h1>%n", res.getPath());

        pw.println("<pre>");
        pw.println("Name                               Last modified                   Size  Description");
        pw.println("<hr>");

        // only draw parent link if the parent is also a fs resource
        Resource parent = ResourceUtil.getParent(res);
        if (parent != null && parent.adaptTo(File.class) != null) {
            pw.println("<a href='..'>Parent Directory</a>");
        }

        // render the children
        renderChildren(pw, res);

        pw.println("</pre>");
        pw.println("</body></html>");
    }

    // ---------- internal

    /**
     * Renders the children of the <code>parent</code> folder to the output.
     */
    private void renderChildren(PrintWriter pw, Resource parent) {
        final Iterator<Resource> children = ResourceUtil.listChildren(parent);
        while ( children.hasNext() ) {
            final Resource current = children.next();

            String name = ResourceUtil.getName(current);

            String displayName = name;
            if (displayName.length() >= 32) {
                displayName = displayName.substring(0, 29).concat("...");
                pw.printf("<a href='%s/%s'>%s</a>", ResourceUtil.getName(parent), name, displayName);
            } else {
                String blanks = NAME_BLANKS.substring(0,
                    32 - displayName.length());
                pw.printf("<a href='%s/%s'>%s</a>%s", ResourceUtil.getName(parent), name, displayName,
                    blanks);
            }

            pw.print("   " + new Date());
            //pw.print("   " + new Date(child.lastModified()));

            pw.print("   ");
            pw.print("-");

            pw.println();
        }
    }
}
