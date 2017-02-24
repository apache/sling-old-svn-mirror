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
package org.apache.sling.fsprovider.internal.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.util.ISO9075;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses JCR XML files that contains content fragments.
 */
class JcrXmlFileParser {
    
    private static final Logger log = LoggerFactory.getLogger(JcrXmlFileParser.class);
    
    private static final SAXParserFactory SAX_PARSER_FACTORY;
    static {
        SAX_PARSER_FACTORY = SAXParserFactory.newInstance();
        SAX_PARSER_FACTORY.setNamespaceAware(true);
    }
    
    private JcrXmlFileParser() {
        // static methods only
    }
    
    /**
     * Parse JSON file.
     * @param file File
     * @return Content
     */
    public static Map<String,Object> parse(File file) {
        log.debug("Parse JCR XML content from {}", file.getPath());
        try (FileInputStream fis = new FileInputStream(file)) {
            XmlHandler xmlHandler = new XmlHandler();
            SAXParser parser = SAX_PARSER_FACTORY.newSAXParser();
            parser.parse(fis, xmlHandler);
            if (xmlHandler.hasError()) {
                throw xmlHandler.getError();
            }
            return xmlHandler.getContent();
        }
        catch (IOException | ParserConfigurationException | SAXException ex) {
            log.warn("Error parsing JCR XML content from " + file.getPath(), ex);
            return null;
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
    static class XmlHandler extends DefaultHandler {
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
                elements.peek().put(decodeName(qName), element);
            }
            elements.push(element);
            
            // get attributes
            for (int i=0; i<attributes.getLength(); i++) {
                element.put(decodeName(attributes.getQName(i)), JcrXmlValueConverter.parseValue(attributes.getValue(i)));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            elements.pop();
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
