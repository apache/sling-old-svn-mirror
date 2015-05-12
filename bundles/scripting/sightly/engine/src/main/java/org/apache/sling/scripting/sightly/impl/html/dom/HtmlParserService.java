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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.impl.html.dom.template.Template;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Properties({
        @Property(name = "service.description", value = "Sightly Simple HTML parser"),
        @Property(name = "service.ranking", intValue = 1000)
})
@Service(HtmlParserService.class)
public class HtmlParserService {

    private static final Logger log = LoggerFactory.getLogger(HtmlParserService.class);

    /**
     * Parse the given document and use the handler to process
     * the markup events
     *
     * @param document - the parsed document
     * @param handler  - a markup handler
     */
    public void parse(String document, MarkupHandler handler) {
        try {
            final StringReader sr = new StringReader(document);
            final TemplateParser parser = new TemplateParser();
            final Template template = parser.parse(sr);
            // walk through the tree and send events
            TreeTraverser tree = new TreeTraverser(handler);
            tree.traverse(template);
        } catch (IOException e) {
            log.error("Failed to parse Sightly template", e);
        }
    }
}
