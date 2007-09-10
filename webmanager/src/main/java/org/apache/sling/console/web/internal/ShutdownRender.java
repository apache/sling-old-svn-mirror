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
package org.apache.sling.console.web.internal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.console.web.Render;

/**
 * The <code>ShutdownRender</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.service
 */
public class ShutdownRender implements Render {

    public static final String NAME = "shutdown";
    public static final String LABEL = null; // hide from navigation
    
    /*
     * (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.Render#getName()
     */
    public String getName() {
        return NAME;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.Render#getLabel()
     */
    public String getLabel() {
        return LABEL;
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.Render#render(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        PrintWriter pw = response.getWriter();
        
        pw.println("<tr>");
        pw.println("<td colspan='2' class='techcontentcell'>");
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
        pw.println("<tr class='content'>");
        pw.println("<th class='content important'>Server terminated</th>");
        pw.println("</tr>");
        pw.println("</table>");
        pw.println("</td>");
        pw.println("</tr>");
    }

}
