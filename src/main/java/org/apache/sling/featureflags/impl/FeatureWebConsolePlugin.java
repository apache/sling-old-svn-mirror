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
package org.apache.sling.featureflags.impl;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.featureflags.ExecutionContext;
import org.apache.sling.featureflags.Feature;

@SuppressWarnings("serial")
public class FeatureWebConsolePlugin extends HttpServlet {

    private final FeatureManager featureManager;

    FeatureWebConsolePlugin(final FeatureManager featureManager) {
        this.featureManager = featureManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final PrintWriter pw = resp.getWriter();
        final Feature[] features = this.featureManager.getAvailableFeatures();
        if (features == null || features.length == 0) {
            pw.println("<p class='statline ui-state-highlight'>No Features currently defined</p>");
        } else {
            pw.printf("<p class='statline ui-state-highlight'>%d Feature(s) currently defined</p>%n", features.length);
            pw.println("<table class='nicetable'>");
            pw.println("<tr><th>Name</th><th>Description</th><th>Enabled</th></tr>");
            final ExecutionContext ctx = createContext(req);
            for (final Feature feature : features) {
                pw.printf("<tr><td>%s</td><td>%s</td><td>%s</td></tr>%n", feature.getName(), feature.getDescription(),
                    feature.isEnabled(ctx));
            }
            pw.println("</table>");
        }
    }

    private ExecutionContext createContext(final HttpServletRequest req) {
        return new ExecutionContext() {

            @Override
            public ResourceResolver getResourceResolver() {
                return null;
            }

            @Override
            public HttpServletRequest getRequest() {
                return req;
            }
        };
    }
}
