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
package org.apache.sling.rewriter.impl.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.html.HtmlParser;
import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.GeneratorFactory;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * On the fly HTML parser which can be used as the
 * starting point for html pipelines.
 *
 */
@Component
@Service(value=GeneratorFactory.class)
@Property(name="pipeline.type",value="html-generator")
public class HtmlGeneratorFactory implements GeneratorFactory {

    @Reference
    private HtmlParser htmlParser;

    /**
     * @see org.apache.sling.rewriter.GeneratorFactory#createGenerator()
     */
    public Generator createGenerator() {
        return new HtmlGenerator(htmlParser);
    }

    public static final class HtmlGenerator implements Generator {

        private final StringWriter writer;

        private final HtmlParser htmlParser;

        private ContentHandler contentHandler;

        public HtmlGenerator(final HtmlParser parser) {
            this.htmlParser = parser;
            this.writer = new StringWriter();
        }

        /**
         * @see org.apache.sling.rewriter.Generator#finished()
         */
        public void finished() throws IOException, SAXException {
            this.htmlParser.parse(new ByteArrayInputStream(this.writer.toString().getBytes("UTF-8")), "UTF-8", this.contentHandler);
        }

        /**
         * @see org.apache.sling.rewriter.Generator#getWriter()
         */
        public PrintWriter getWriter() {
            return new PrintWriter(writer);
        }

        public void init(ProcessingContext context,
                         ProcessingComponentConfiguration config)
        throws IOException {
            // nothing to do
        }

        /**
         * @see org.apache.sling.rewriter.Generator#setContentHandler(org.xml.sax.ContentHandler)
         */
        public void setContentHandler(ContentHandler handler) {
            this.contentHandler = handler;
        }

        /**
         * @see org.apache.sling.rewriter.Generator#dispose()
         */
        public void dispose() {
            // nothing to do
        }
    }
}