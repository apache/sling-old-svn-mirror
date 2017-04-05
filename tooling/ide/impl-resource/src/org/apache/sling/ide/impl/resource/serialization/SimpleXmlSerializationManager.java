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
package org.apache.sling.ide.impl.resource.serialization;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.sling.ide.serialization.SerializationData;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.ResourceProxy;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

public class SimpleXmlSerializationManager implements SerializationManager, SerializationDataBuilder {

    private static final String TAG_PROPERTY = "property";
    private static final String ATT_PROPERTY_NAME = "name";
    private static final String TAG_RESOURCE = "resource";

    private static final String CONTENT_XML = ".content.xml";

    @Override
    public boolean isSerializationFile(String filePath) {
        return filePath.endsWith(CONTENT_XML);
    }

    @Override
    public String getSerializationFilePath(String baseFilePath, SerializationKind serializationKind) {
        return baseFilePath + File.separatorChar + CONTENT_XML;
    }

    @Override
    public String getBaseResourcePath(String serializationFilePath) {
        if (!serializationFilePath.endsWith(CONTENT_XML)) {
            throw new IllegalArgumentException("File path " + serializationFilePath + "does not end with '"
                    + File.separatorChar + CONTENT_XML + "'");
        }

        if (CONTENT_XML.equals(serializationFilePath)) {
            return "";
        }

        return serializationFilePath.substring(0, serializationFilePath.length() - (CONTENT_XML.length() + 1));
    }

    @Override
    public ResourceProxy readSerializationData(String filePath, InputStream source) throws IOException {

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            SerializationDataHandler h = new SerializationDataHandler();

            saxParser.parse(new InputSource(source), h);

            return new ResourceProxy(filePath, h.getResult());
        } catch (ParserConfigurationException | SAXException e) {
            // TODO proper exception handling
            throw new RuntimeException(e);
        }

    }
    
    @Override
    public SerializationDataBuilder newBuilder(Repository repository,
    		File contentSyncRoot) throws SerializationException {
    	return this;
    }

    @Override
    public SerializationData buildSerializationData(File contentSyncRoot, ResourceProxy resource)
            throws SerializationException {

        if (resource == null) {
            return null;
        }

        Map<String, Object> content = resource.getProperties();

        if (content == null || content.isEmpty()) {
            return null;
        }

        try {
            SAXTransformerFactory f = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            StreamResult sr = new StreamResult(result);

            TransformerHandler handler = f.newTransformerHandler();
            Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            handler.setResult(sr);
            handler.startDocument();
            startElement(handler, TAG_RESOURCE);
            Set<Entry<String, Object>> entrySet = new TreeMap<>(content).entrySet();
            for (Map.Entry<String, Object> property : entrySet) {
                Object value = property.getValue();
                if (value instanceof String) {
                    String tagName = property.getKey();
                    String tagValue = (String) value;
                    AttributesImpl attributes = new AttributesImpl();
                    attributes.addAttribute("", ATT_PROPERTY_NAME, ATT_PROPERTY_NAME, null, tagName);
                    handler.startElement("", TAG_PROPERTY, TAG_PROPERTY, attributes);
                    handler.characters(tagValue.toCharArray(), 0, tagValue.length());
                    handler.endElement("", TAG_PROPERTY, TAG_PROPERTY);
                } else {
                    // TODO multi-valued properties, other primitives
                    System.err.println("Can't yet handle property " + property.getKey() + " of type "
                            + value.getClass());
                }
            }

            endElement(handler, TAG_RESOURCE);
            handler.endDocument();

            // TODO - also add the serialization type
            return new SerializationData(resource.getPath(), CONTENT_XML, result.toByteArray(), null);
        } catch (TransformerConfigurationException | TransformerFactoryConfigurationError | SAXException e) {
            // TODO proper exception handling
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getRepositoryPath(String osPath) {
        return osPath;
    }

    @Override
    public String getOsPath(String repositoryPath) {
        return repositoryPath;
    }

    private void startElement(TransformerHandler handler, String tagName) throws SAXException {

        handler.startElement("", tagName, tagName, null);
    }

    private void endElement(TransformerHandler handler, String tagName) throws SAXException {

        handler.endElement("", tagName, tagName);
    }

    /* (non-Javadoc)
     * @see org.apache.sling.ide.serialization.SerializationManager#destroy()
     */
    public void destroy() {
    }

    static class SerializationDataHandler extends DefaultHandler {
        private Map<String, Object> result;
        private String propertyName;

        @Override
        public void startDocument() throws SAXException {
            result = new HashMap<>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

            if (TAG_PROPERTY.equals(qName)) {
                propertyName = attributes.getValue(ATT_PROPERTY_NAME);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {

            if (propertyName != null) {
                result.put(propertyName, new String(ch, start, length));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {

            if (TAG_PROPERTY.equals(qName)) {
                propertyName = null;
            }
        }

        public Map<String, Object> getResult() {
            return result;
        }
    }
}
