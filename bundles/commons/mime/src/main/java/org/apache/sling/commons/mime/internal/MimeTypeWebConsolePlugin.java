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
package org.apache.sling.commons.mime.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class MimeTypeWebConsolePlugin extends HttpServlet {

    /** Serial Version */
    private static final long serialVersionUID = -2025952303202431607L;

    static final String LABEL = "mimetypes";

    static final String TITLE = "MIME Types";

    static final String RES_LOC = LABEL + "/res";

    static final String CSS_REFS = RES_LOC + "/jquery.treeTable.css";

    private final MimeTypeServiceImpl mimeTypeService;

    MimeTypeWebConsolePlugin(MimeTypeServiceImpl mimeTypeService) {
        this.mimeTypeService = mimeTypeService;
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Map<String, Set<String>> mimetab = new TreeMap<String, Set<String>>();

        Map<String, String> extMap = mimeTypeService.getExtensionMap();

        int numExt = 0;
        for (Entry<String, String> entry : mimeTypeService.getMimeMap().entrySet()) {
            String ext = entry.getKey();
            String mime = entry.getValue();

            Set<String> extList = mimetab.get(mime);
            if (extList == null) {
                extList = new HashSet<String>();
                mimetab.put(mime, extList);
            }

            if (ext.equals(extMap.get(mime))) {
                ext = "*" + ext + "*";
            }

            extList.add(ext);

            numExt++;
        }

        PrintWriter pw = response.getWriter();

        pw.println("<script type='text/javascript' src='" + RES_LOC
            + "/jquery.treeTable.min.js'></script>");
        pw.println("<script type='text/javascript'>");
        pw.println("  $(document).ready(function()  {");
        pw.println("    $('#mimetabtable').treeTable({ treeColumn: 1 });");
        pw.println("  });");
        pw.println("</script>");

        pw.println("<div id='plugin_content'>");
        pw.println("<div class='fullwidth'>");
        pw.println("<div class='statusline'>Statistic: " + mimetab.size()
            + " MIME Types, " + numExt + " Extensions</div>");
        pw.println("</div>");

        pw.println("<div class='table'>");
        pw.println("<table id='mimetabtable' class='tablelayout'>");

        pw.println("<colgroup>");
        pw.println("<col width='20px'>");
        pw.println("<col width='50%'>");
        pw.println("<col width='50%'>");
        pw.println("</colgroup>");

        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th colspan='2'>Mime Type</th>");
        pw.println("<th>Extensions</th>");
        pw.println("</tr>");
        pw.println("</thead>");

        pw.println("<tbody>");

        String currentMajor = null;

        for (Entry<String, Set<String>> entry : mimetab.entrySet()) {
            String major = getMajorType(entry.getKey());

            if (!major.equals(currentMajor)) {
                currentMajor = major;
                pw.println("<tr id='" + currentMajor + "'>");
                pw.println("<td>&nbsp;</td>");
                pw.println("<td>" + currentMajor + "</td>");
                pw.println("<td>--</td>");
                pw.println("</tr>");
            }

            pw.println("<tr id='" + entry.getKey().replace('/', '-')
                + "' class='child-of-" + currentMajor + "'>");
            pw.println("<td>&nbsp;</td>");
            pw.println("<td>" + entry.getKey() + "</td>");
            pw.println("<td>" + entry.getValue() + "</td>");
            pw.println("</tr>");
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("</div>");
        pw.println("</div>");
    }

    private String getMajorType(String type) {
        int slash = type.indexOf('/');
        return (slash > 0) ? type.substring(0, slash) : type;
    }

    /**
     * Resource Provider method for the Web Console 2 integration
     * @param resourcePath
     * @return
     */
    @SuppressWarnings("unused")
    private URL getResource(final String resourcePath) {

        final int rPi = resourcePath.indexOf("/res/");
        if (rPi >= 0) {
            final String path = resourcePath.substring(rPi);
            return getClass().getResource(path);
        }

        return null;
    }

}
