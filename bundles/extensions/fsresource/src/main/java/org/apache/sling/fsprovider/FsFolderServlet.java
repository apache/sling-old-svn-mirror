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
package org.apache.sling.fsprovider;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * The <code>FsFolderServlet</code> lists the files and folders of a folder
 * mapped into the Sling resource tree. The listing produced is similar to the
 * default index listing produced by Apache httpd.
 * 
 * @scr.component immediate="true" metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="FileSystem Folder Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.resourceTypes"
 *               valueRef="FsProviderConstants.RESOURCE_TYPE_FOLDER"
 */
public class FsFolderServlet extends SlingSafeMethodsServlet {

    // a number of blanks used to format the listing of folder entries
    private static final String NAME_BLANKS = "                                  ";

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        // if the request URL is not terminated with a slash, redirect to the
        // same URL with a trailing slash (this makes preparing the response
        // easier
        if (!request.getRequestURI().endsWith("/")) {
            response.sendRedirect(request.getRequestURL() + "/");
            return;
        }

        // ensure the resource adapts to a filesystem folder; generally
        // this should be the case, but we never know whether someone really
        // creates a JCR resource with the fs provider folder resource type
        Resource res = request.getResource();
        File file = res.adaptTo(File.class);
        if (file == null || !file.isDirectory()) {
            throw new ResourceNotFoundException(
                request.getResource().getPath(),
                "Resource is not a file system folder");
        }

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
        renderChildren(pw, file);

        pw.println("</pre>");
        pw.println("</body></html>");
    }

    // ---------- internal

    /**
     * Renders the children of the <code>parent</code> folder to the output.
     */
    private void renderChildren(PrintWriter pw, File parent) {
        File[] children = parent.listFiles();
        if (children != null && children.length > 0) {
            Arrays.sort(children, FileNameComparator.INSTANCE);

            for (File child : children) {

                String name = child.getName();
                if (child.isDirectory()) {
                    name = name.concat("/");
                }

                String displayName = name;
                if (displayName.length() >= 32) {
                    displayName = displayName.substring(0, 29).concat("...");
                    pw.printf("<a href='%s'>%s</a>", name, displayName);
                } else {
                    String blanks = NAME_BLANKS.substring(0,
                        32 - displayName.length());
                    pw.printf("<a href='%s'>%s</a>%s", name, displayName,
                        blanks);
                }

                pw.print("   " + new Date(child.lastModified()));

                pw.print("   ");
                if (child.isFile()) {
                    pw.print(child.length());
                } else {
                    pw.print("-");
                }

                pw.println();
            }
        }
    }

    // order files by type (folder before files) and name (case insensitive)
    private static class FileNameComparator implements Comparator<File> {

        public static final FileNameComparator INSTANCE = new FileNameComparator();

        public int compare(File f1, File f2) {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            }

            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

}
