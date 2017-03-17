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
package org.apache.sling.jcr.contentparser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.jcr.contentparser.Content;
import org.apache.sling.jcr.contentparser.ContentParser;
import org.apache.sling.jcr.contentparser.ParseException;
import org.apache.sling.jcr.contentparser.ParserOptions;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses JCR XML files that contains content fragments.
 * Instance of this class is thread-safe.
 */
public final class JcrXmlContentParser implements ContentParser {
    
    private final ParserHelper helper;    
    private final SAXParserFactory saxParserFactory;
    
    public JcrXmlContentParser(ParserOptions options) {
        this.helper = new ParserHelper(options);
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
    }
    
    @Override
    public Content parse(InputStream is) throws IOException, ParseException {
        try {
            XmlHandler xmlHandler = new XmlHandler();
            SAXParser parser = saxParserFactory.newSAXParser();
            parser.parse(is, xmlHandler);
            if (xmlHandler.hasError()) {
                throw xmlHandler.getError();
            }
            return xmlHandler.getContent();
        }
        catch (ParserConfigurationException | SAXException ex) {
            throw new ParseException("Error parsing JCR XML content.", ex);
        }
    }
    
    /**
     * Decodes element or attribute names.
     * @param qname qname
     * @return Decoded name
     */
    static String decodeName(String qname) {
        return ISO9075.decode(qname);
    }
    
    /**
     * Parses XML stream to Map.
     */
    class XmlHandler extends DefaultHandler {
        private final Content root = new ContentImpl(null);
        private final Deque<Content> elements = new ArrayDeque<>();
        private SAXParseException error;
        
        public Content getContent() {
            return root;
        }
        
        public boolean hasError() {
            return error != null;
        }
        
        public SAXParseException getError() {
            return error;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            
            // prepare new element
            Content element;
            if (elements.isEmpty()) {
                element = root;
            }
            else {
                String resourceName = decodeName(qName);
                element = new ContentImpl(resourceName);
                if (!helper.ignoreResource(resourceName)) {
                    elements.peek().getChildren().put(resourceName, element);
                }
            }
            elements.push(element);
            
            // get attributes
            Map<String,Object> properties = element.getProperties();
            for (int i=0; i<attributes.getLength(); i++) {
                String propertyName = helper.cleanupPropertyName(decodeName(attributes.getQName(i)));
                if (!helper.ignoreProperty(propertyName)) {
                    Object value = JcrXmlValueConverter.parseValue(propertyName, attributes.getValue(i));
                    if (value != null) {
                        properties.put(propertyName, value);
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            Content element = elements.pop();
            helper.ensureDefaultPrimaryType(element);
        }

        @Override
        public void error(SAXParseException ex) throws SAXException {
            this.error = ex;
        }

        @Override
        public void fatalError(SAXParseException ex) throws SAXException {
            this.error = ex;
        }
        
    }
    
}
