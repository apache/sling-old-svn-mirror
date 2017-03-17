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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.jcr.contentparser.ContentHandler;
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
    public void parse(ContentHandler handler, InputStream is) throws IOException, ParseException {
        try {
            XmlHandler xmlHandler = new XmlHandler(handler);
            SAXParser parser = saxParserFactory.newSAXParser();
            parser.parse(is, xmlHandler);
            if (xmlHandler.hasError()) {
                throw xmlHandler.getError();
            }
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
        private final ContentHandler contentHandler;
        private final Deque<String> paths = new ArrayDeque<>();
        private final Set<String> ignoredPaths = new HashSet<>();
        private SAXParseException error;
        
        public XmlHandler(ContentHandler contentHandler) {
            this.contentHandler = contentHandler;
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
            
            String resourceName = decodeName(qName);

            // generate path for element
            String path;
            if (paths.isEmpty()) {
                path = "/";
            }
            else {
                path = helper.concatenatePath(paths.peek(), resourceName);
                if (helper.ignoreResource(resourceName)) {
                    ignoredPaths.add(path);
                }
            }
            paths.push(path);
            
            // skip further processing if this path or a parent path is ignored
            if (isIgnoredPath(path)) {
                return;
            }
            
            // get properties
            Map<String,Object> properties = new HashMap<>();
            for (int i=0; i<attributes.getLength(); i++) {
                String propertyName = helper.cleanupPropertyName(decodeName(attributes.getQName(i)));
                if (!helper.ignoreProperty(propertyName)) {
                    Object value = JcrXmlValueConverter.parseValue(propertyName, attributes.getValue(i));
                    if (value != null) {
                        properties.put(propertyName, value);
                    }
                }
            }
            helper.ensureDefaultPrimaryType(properties);
            contentHandler.resource(path, properties);
        }
        
        private boolean isIgnoredPath(String path) {
            if (StringUtils.isEmpty(path)) {
                return false;
            }
            if (ignoredPaths.contains(path)) {
                return true;
            }
            String parentPath = StringUtils.substringBeforeLast(path, "/");
            return isIgnoredPath(parentPath);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            paths.pop();
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
