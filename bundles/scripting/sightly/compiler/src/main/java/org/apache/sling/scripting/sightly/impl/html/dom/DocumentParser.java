/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.html.dom;

import java.io.IOException;
import java.io.StringReader;

import org.apache.sling.scripting.sightly.impl.html.dom.template.Template;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentParser.class);

    /**
     * Parse the given document and use the handler to process
     * the markup events
     *
     * @param document - the parsed document
     * @param handler  - a markup handler
     */
    public static void parse(String document, MarkupHandler handler) {
        try {
            final StringReader sr = new StringReader(document);
            final TemplateParser parser = new TemplateParser();
            final Template template = parser.parse(sr);
            // walk through the tree and send events
            TreeTraverser tree = new TreeTraverser(handler);
            tree.traverse(template);
        } catch (IOException e) {
            LOGGER.error("Failed to parse HTL template", e);
        }
    }
}
