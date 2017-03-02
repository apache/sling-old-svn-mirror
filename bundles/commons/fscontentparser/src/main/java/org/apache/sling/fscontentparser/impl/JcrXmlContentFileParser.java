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
package org.apache.sling.fscontentparser.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.fscontentparser.ContentFileParser;
import org.apache.sling.fscontentparser.ParseException;
import org.apache.sling.fscontentparser.ParserOptions;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses JCR XML files that contains content fragments.
 * Instance of this class is thread-safe.
 */
public final class JcrXmlContentFileParser implements ContentFileParser {
    
    private final ParserHelper helper;    
    private final SAXParserFactory saxParserFactory;
    
    public JcrXmlContentFileParser(ParserOptions options) {
        this.helper = new ParserHelper(options);
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
    }
    
    @Override
    public Map<String,Object> parse(File file) throws IOException, ParseException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return parse(fis);
        }
    }
    
    @Override
    public Map<String,Object> parse(InputStream is) throws IOException, ParseException {
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
        private final Map<String,Object> content = new LinkedHashMap<>();
        private final Stack<Map<String,Object>> elements = new Stack<>();
        private SAXParseException error;
        
        public Map<String,Object> getContent() {
            return content;
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
            
            // prepare map for element
            Map<String,Object> element;
            if (elements.isEmpty()) {
                element = content;
            }
            else {
                element = new HashMap<>();
                String resourceName = decodeName(qName);
                if (!helper.ignoreResource(resourceName)) {
                    elements.peek().put(resourceName, element);
                }
            }
            elements.push(element);
            
            // get attributes
            for (int i=0; i<attributes.getLength(); i++) {
                String propertyName = helper.cleanupPropertyName(decodeName(attributes.getQName(i)));
                if (!helper.ignoreProperty(propertyName)) {
                    Object value = JcrXmlValueConverter.parseValue(propertyName, attributes.getValue(i));
                    if (value != null) {
                        element.put(propertyName, value);
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            Map<String,Object> element = elements.pop();
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
