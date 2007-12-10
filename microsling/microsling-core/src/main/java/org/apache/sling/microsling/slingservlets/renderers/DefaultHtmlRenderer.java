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
package org.apache.sling.microsling.slingservlets.renderers;

import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.microsling.resource.SyntheticResourceData;

/** Reusable default HTML rendering of a Node
 */
public class DefaultHtmlRenderer {

    public void render(PrintWriter pw, Resource r, Node n) throws RepositoryException {
        pw.println("<h1>Node dumped by " + getClass().getSimpleName() + "</h1>");
        pw.println("<p>Node path: <b>" + n.getPath() + "</b></p>");
        pw.println("<p>Resource metadata: <b>" + r.getResourceMetadata() + "</b></p>");

        pw.println("<h2>Node properties</h2>");
        for (PropertyIterator pi = n.getProperties(); pi.hasNext();) {
            final Property p = pi.nextProperty();
            printPropertyValue(pw, p);
        }
    }

    public void render(PrintWriter pw, Resource r, SyntheticResourceData data) {
        pw.println("<h1>SyntheticResourceData</h1>");
        pw.println("<p>" + data.toString() + "</p>");
    }

    protected void dump(PrintWriter pw, Resource r, Property p) throws RepositoryException {
        pw.println("<h2>Property dumped by " + getClass().getSimpleName() + "</h1>");
        pw.println("<p>Property path:" + p.getPath() + "</p>");
        pw.println("<p>Resource metadata: " + r.getResourceMetadata() + "</p>");

        printPropertyValue(pw, p);
    }

    protected void printPropertyValue(PrintWriter pw, Property p)
            throws RepositoryException {

        pw.print(p.getName() + ": <b>");

        if (p.getDefinition().isMultiple()) {
            Value[] values = p.getValues();
            pw.print('[');
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                pw.print(values[i].getString());
            }
            pw.print(']');
        } else {
            pw.print(p.getValue().getString());
        }

        pw.print("</b><br/>");
    }
}
