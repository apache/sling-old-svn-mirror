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
package org.apache.sling.bgservlets.impl;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.bgservlets.impl.nodestream.NodeInputStream;

/** Servlet that replays the output of servlets executed in
 *  the background.
 */
@Component
@Service
@SuppressWarnings("serial")
@Properties ( {
    @Property(name="sling.servlet.resourceTypes", value="sling/servlet/default"),
    @Property(name="sling.servlet.extensions", value="bgreplay")
})
public class OutputReplayServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException {
        final Node n = request.getResource().adaptTo(Node.class);
        if(n == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                    "Resource does not adapt to a Node: " + request.getResource().getPath());
        }
        
        // TODO content-type, length etc
        final NodeInputStream nis = new NodeInputStream(n);
        try {
            final OutputStream os = response.getOutputStream();
            final byte [] buffer = new byte[32768];
            int count = 0;
            while((count = nis.read(buffer, 0, buffer.length)) > 0) {
                os.write(buffer, 0, count);
            }
            os.flush();
        } finally {
            if(nis != null) {
                nis.close();
            }
        }
    }
}
