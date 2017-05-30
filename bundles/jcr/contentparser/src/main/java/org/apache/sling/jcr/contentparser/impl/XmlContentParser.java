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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.jcr.contentparser.ContentHandler;
import org.apache.sling.jcr.contentparser.ContentParser;
import org.apache.sling.jcr.contentparser.ParseException;
import org.apache.sling.jcr.contentparser.ParserOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parses XML files that contains content fragments.
 * Instance of this class is thread-safe.
 */
public final class XmlContentParser implements ContentParser {
    
    private final ParserHelper helper;    
    private final DocumentBuilderFactory documentBuilderFactory;
    
    public XmlContentParser(ParserOptions options) {
        this.helper = new ParserHelper(options);
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
    }
    
    @Override
    public void parse(ContentHandler handler, InputStream is) throws IOException, ParseException {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(is);
            parse(handler, doc.getDocumentElement(), null);
        }
        catch (ParserConfigurationException | SAXException ex) {
            throw new ParseException("Error parsing JCR XML content.", ex);
        }
    }
    
    private void parse(ContentHandler handler, Element element, String parentPath) {
        
        // build node path
        String path;
        if (parentPath == null) {
            path = "/";
        }
        else {
            String name = getChildText(element, "name");
            if (StringUtils.isEmpty(name)) {
                throw new ParseException("Child node without name detected below path " + parentPath);
            }
            path = helper.concatenatePath(parentPath, name);
            if (helper.ignoreResource(name)) {
                return;
            }
        }

        Map<String,Object> properties = new HashMap<>();
        
        // primary node type and mixins
        String primaryType = getChildText(element, "primaryNodeType");
        if (StringUtils.isNotBlank(primaryType) && !helper.ignoreProperty("jcr:primaryType")) {
            properties.put("jcr:primaryType", primaryType);
        }
        String[] mixins = getChildTextArray(element, "mixinNodeType");
        if (mixins.length > 0 && !helper.ignoreProperty("jcr:mixinTypes")) {
            properties.put("jcr:mixinTypes", mixins);
        }
        
        // properties
        List<Element> propertyElements = getChildren(element, "property");
        for (Element propertyElement : propertyElements) {
            
            // property name
            String name = getChildText(propertyElement, "name");
            if (StringUtils.isBlank(name)) {
                throw new ParseException("Property without name detected at path " + path);
            }
            if (helper.ignoreProperty(name)) {
                continue;
            }
            
            // property type
            String typeString = getChildText(propertyElement, "type");
            if (StringUtils.isBlank(typeString)) {
                throw new ParseException("Property '" + name + "' has no type at path " + path);
            }
            int type;
            try {
                type = PropertyType.valueFromName(typeString);
            }
            catch (IllegalArgumentException ex) {
                throw new ParseException("Property '" + name + "' has illegal type '" + typeString + "' at path " + path);
            }
            
            // property value
            Object value;
            List<Element> valuesElements = getChildren(propertyElement, "values");
            if (!valuesElements.isEmpty()) {
                Element valuesElement = valuesElements.get(0);
                List<Element> valueElements = getChildren(valuesElement, "value");
                String[] stringValues = new String[valueElements.size()];
                for (int i=0; i<valueElements.size(); i++) {
                    stringValues[i] = valueElements.get(i).getTextContent();
                }
                value = convertMultiValue(stringValues, type);
            }
            else {
                String stringValue = getChildText(propertyElement, "value");
                value = convertValue(stringValue, type);
            }
            properties.put(name, value);
        }
        
        // report current JSON object
        helper.ensureDefaultPrimaryType(properties);
        handler.resource(path, properties);
        
        // child nodes
        List<Element> nodeElements = getChildren(element, "node");
        for (Element node : nodeElements) {
            parse(handler, node, path);
        }
        
    }
    
    private List<Element> getChildren(Element element, String childName) {
        List<Element> result = new ArrayList<>();
        NodeList children = element.getChildNodes();
        int len = children.getLength();
        for (int i=0; i<len; i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElement = (Element)child;
                if (StringUtils.equals(childElement.getNodeName(), childName)) {
                    result.add(childElement);
                }
            }
        }
        return result;
    }
    
    private String getChildText(Element element, String childName) {
        List<Element> children = getChildren(element, childName);
        if (children.isEmpty()) {
            return null;
        }
        else if (children.size() == 1) {
            return children.get(0).getTextContent();
        }
        else {
            throw new ParseException("Found multiple elements with name '" + childName + "': " + children.size());
        }
    }
    
    private String[] getChildTextArray(Element element, String childName) {
        List<Element> children = getChildren(element, childName);
        String[] result = new String[children.size()];
        for (int i=0; i<children.size(); i++) {
            result[i] = children.get(i).getTextContent();
        }
        return result;
    }
    
    private Object convertValue(String value, int type) {
        switch (type) {
            case PropertyType.STRING:
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
            case PropertyType.URI:
                return value;
            case PropertyType.LONG:
                return Long.valueOf(value);
            case PropertyType.DOUBLE:
                return Double.valueOf(value);
            case PropertyType.DATE:
                return helper.tryParseCalendar(value);
            case PropertyType.BOOLEAN:
                return Boolean.valueOf(value);
            case PropertyType.DECIMAL:
                return new BigDecimal(value);
            default:
                throw new ParseException("Unsupported property type: " + PropertyType.nameFromValue(type));
        }
    }

    private Object convertMultiValue(String[] values, int type) {
        Object[] result = new Object[values.length];
        for (int i=0; i<values.length; i++) {
            result[i] = convertValue(values[i], type);
        }
        return helper.convertSingleTypeArray(result);
    }
    
}
