/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.webconsole.impl;

import java.io.PrintWriter;

import org.apache.sling.api.request.ResponseUtil;

/** Webconsole plugin to execute health check rules */ 
class WebConsoleHelper {
    
    final PrintWriter pw;
    
    WebConsoleHelper(PrintWriter w) {
        pw = w;
    }

    PrintWriter writer() {
        return pw;
    }
    
    void tdContent() {
        pw.print("<td class='content' colspan='2'>");
    }

    void closeTd() {
        pw.print("</td>");
    }

    void closeTr() {
        pw.println("</tr>");
    }

    void tdLabel(final String label) {
        pw.println("<td class='content'>" + ResponseUtil.escapeXml(label) + "</td>");
    }

    void tr() {
        pw.println("<tr class='content'>");
    }

    void titleHtml(String title, String description) {
        tr();
        pw.println("<th colspan='3' class='content container'>" + ResponseUtil.escapeXml(title) + "</th>");
        closeTr();

        if (description != null) {
            tr();
            pw.println("<td colspan='3' class='content'>" +ResponseUtil.escapeXml(description) + "</th>");
            closeTr();
        }
    }
}
