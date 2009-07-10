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
package org.apache.sling.rewriter.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.SerializerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * This sax serializer serializes html-
 * @scr.component metatype="no"
 * @scr.service
 * @scr.property name="pipeline.type" value="html-serializer"
 */
public class HtmlSerializerFactory implements SerializerFactory {

    private static final List<String> DEFAULT_EMPTY_TAGS;
    static {
        DEFAULT_EMPTY_TAGS = new ArrayList<String>();
        DEFAULT_EMPTY_TAGS.add("br");
        DEFAULT_EMPTY_TAGS.add("area");
        DEFAULT_EMPTY_TAGS.add("link");
        DEFAULT_EMPTY_TAGS.add("img");
        DEFAULT_EMPTY_TAGS.add("param");
        DEFAULT_EMPTY_TAGS.add("hr");
        DEFAULT_EMPTY_TAGS.add("input");
        DEFAULT_EMPTY_TAGS.add("col");
        DEFAULT_EMPTY_TAGS.add("base");
        DEFAULT_EMPTY_TAGS.add("meta");
    }

    /**
     * @see org.apache.sling.rewriter.SerializerFactory#createSerializer()
     */
    public Serializer createSerializer() {
        return new HtmlSerializer();
    }

    public class HtmlSerializer implements Serializer {

        private PrintWriter delegatee;

        private List<String> emptyTags;

        /**
         * @see org.apache.sling.rewriter.Serializer#init(org.apache.sling.rewriter.ProcessingContext, org.apache.sling.rewriter.ProcessingComponentConfiguration)
         */
        public void init(ProcessingContext pipelineContext, ProcessingComponentConfiguration config)
        throws IOException {
            final PrintWriter writer = pipelineContext.getWriter();
            if (writer == null) {
                throw new IllegalArgumentException("Writer must not be null");
            }
            this.delegatee = writer;
            this.emptyTags = DEFAULT_EMPTY_TAGS;
        }


        /**
         * @see org.xml.sax.ContentHandler#endDocument()
         */
        public void endDocument() throws SAXException {
            this.delegatee.flush();
        }

        /**
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        public void startElement(String uri, String localName, String name,
                Attributes atts) throws SAXException {
            boolean endSlash = false;
            this.delegatee.write('<');
            this.delegatee.write(localName);
            final String quotesString = atts.getValue(HtmlGeneratorFactory.NAMESPACE, HtmlGeneratorFactory.QUOTES_ATTR);
            for(int i=0; i<atts.getLength(); i++) {
                if (HtmlGeneratorFactory.END_SLASH_ATTR.equals(atts.getQName(i))) {
                    endSlash = true;
                } else if (!HtmlGeneratorFactory.NAMESPACE.equals(atts.getURI(i))) {
                    this.delegatee.write(' ');
                    this.delegatee.write(atts.getLocalName(i));
                    final String value = atts.getValue(i);
                    if ( value != null ) {
                        this.delegatee.write('=');
                        final char quoteChar;
                        if ( quotesString != null && quotesString.length() > i ) {
                            quoteChar = quotesString.charAt(i);
                        } else {
                            quoteChar = '\"';
                        }
                        this.delegatee.write(quoteChar);
                        this.delegatee.write(value);
                        this.delegatee.write(quoteChar);
                    }
                }
            }

            if (endSlash) {
                // XHTML
                this.delegatee.write("/");
            }

            this.delegatee.write(">");
        }

        /**
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        public void endElement(String uri, String localName, String name)
                throws SAXException {
            if (!emptyTags.contains(localName)) {
                this.delegatee.write("</");
                this.delegatee.write(localName);
                this.delegatee.write('>');
            }
        }


        /**
         * Called by HtmlParser if character data and tags are to be output for which no
         * special handling is necessary.
         *
         * @param buffer Character data
         * @param offset Offset where character data starts
         * @param length The length of the character data
         */
        public void characters(char[] buffer, int offset, int length)
        throws SAXException {
            //this.checkStartElement(false);

            // special hack for flush request, see bug #20068
            if (length == 0) {
                this.delegatee.flush();
            } else {
                this.delegatee.write(buffer, offset, length);
            }
        }

        /**
         * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
         */
        public void endPrefixMapping(String prefix) throws SAXException {
            // not used atm
        }

        /**
         * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
         */
        public void ignorableWhitespace(char[] ch, int start, int length)
                throws SAXException {
            // not used atm
        }

        /**
         * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
         */
        public void processingInstruction(String target, String data)
                throws SAXException {
            // not used atm
        }

        /**
         * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
         */
        public void setDocumentLocator(Locator locator) {
            // not used atm
        }

        /**
         * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
         */
        public void skippedEntity(String name) throws SAXException {
            // not used atm
        }

        /**
         * @see org.xml.sax.ContentHandler#startDocument()
         */
        public void startDocument() throws SAXException {
            // not used atm
        }


        /**
         * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
         */
        public void startPrefixMapping(String prefix, String uri)
                throws SAXException {
            // not used atm
        }

        /**
         * @see org.apache.sling.rewriter.Serializer#dispose()
         */
        public void dispose() {
            // nothing to do
        }
    }
}
