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
package org.apache.sling.testing.mock.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Helper methods to parse OSGi metadata.
 */
final class OsgiMetadataUtil {

    private static final Logger log = LoggerFactory.getLogger(OsgiMetadataUtil.class);

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;
    static {
        DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
        DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
    }

    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    private static final BiMap<String, String> NAMESPACES = HashBiMap.create();
    static {
        NAMESPACES.put("scr", "http://www.osgi.org/xmlns/scr/v1.1.0");
    }

    private OsgiMetadataUtil() {
        // static methods only
    }

    private static final NamespaceContext NAMESPACE_CONTEXT = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            return NAMESPACES.get(prefix);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return NAMESPACES.inverse().get(namespaceURI);
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return NAMESPACES.keySet().iterator();
        }
    };
    
    public static String getMetadataPath(Class clazz) {
        return "/OSGI-INF/" + StringUtils.substringBefore(clazz.getName(), "$") + ".xml";
    }

    /**
     * Try to read OSGI-metadata from /OSGI-INF and read all implemented
     * interfaces and service properties
     * @param clazz OSGi service implementation class
     * @return Metadata document or null
     */
    public static Document getMetadata(Class clazz) {
        String metadataPath = getMetadataPath(clazz);
        InputStream metadataStream = clazz.getResourceAsStream(metadataPath);
        if (metadataStream == null) {
            log.debug("No OSGi metadata found at {}", metadataPath);
            return null;
        }
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(metadataStream);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("Unable to read classpath resource: " + metadataPath, ex);
        } catch (SAXException ex) {
            throw new RuntimeException("Unable to read classpath resource: " + metadataPath, ex);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read classpath resource: " + metadataPath, ex);
        } finally {
            try {
                metadataStream.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    public static Set<String> getServiceInterfaces(Class clazz, Document metadata) {
        Set<String> serviceInterfaces = new HashSet<String>();
        String query = "/components/component[@name='" + clazz.getName() + "']/service/provide[@interface!='']";
        NodeList nodes = queryNodes(metadata, query);
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String serviceInterface = getAttributeValue(node, "interface");
                if (StringUtils.isNotBlank(serviceInterface)) {
                    serviceInterfaces.add(serviceInterface);
                }
            }
        }
        return serviceInterfaces;
    }

    public static Map<String, Object> getProperties(Class clazz, Document metadata) {
        Map<String, Object> props = new HashMap<String, Object>();
        String query = "/components/component[@name='" + clazz.getName() + "']/property[@name!='' and @value!='']";
        NodeList nodes = queryNodes(metadata, query);
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String name = getAttributeValue(node, "name");
                String value = getAttributeValue(node, "value");
                String type = getAttributeValue(node, "type");
                if (StringUtils.equals("Integer", type)) {
                    props.put(name, Integer.parseInt(value));
                } else {
                    props.put(name, value);
                }
            }
        }
        return props;
    }

    public static List<Reference> getReferences(Class clazz, Document metadata) {
        List<Reference> references = new ArrayList<Reference>();
        String query = "/components/component[@name='" + clazz.getName() + "']/reference[@name!='']";
        NodeList nodes = queryNodes(metadata, query);
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                references.add(new Reference(node));
            }
        }
        return references;
    }

    public static String getActivateMethodName(Class clazz, Document metadata) {
        return getLifecycleMethodName(clazz, metadata, "activate");
    }

    public static String getDeactivateMethodName(Class clazz, Document metadata) {
        return getLifecycleMethodName(clazz, metadata, "deactivate");
    }

    public static String getModifiedMethodName(Class clazz, Document metadata) {
        return getLifecycleMethodName(clazz, metadata, "modified");
    }

    private static String getLifecycleMethodName(Class clazz, Document metadata, String methodName) {
        String query = "/components/component[@name='" + clazz.getName() + "']";
        Node node = queryNode(metadata, query);
        if (node != null) {
            return getAttributeValue(node, methodName);
        }
        return null;
    }

    private static NodeList queryNodes(Document metadata, String xpathQuery) {
        try {
            XPath xpath = XPATH_FACTORY.newXPath();
            xpath.setNamespaceContext(NAMESPACE_CONTEXT);
            return (NodeList) xpath.evaluate(xpathQuery, metadata, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            throw new RuntimeException("Error evaluating XPath: " + xpathQuery, ex);
        }
    }

    private static Node queryNode(Document metadata, String xpathQuery) {
        try {
            XPath xpath = XPATH_FACTORY.newXPath();
            xpath.setNamespaceContext(NAMESPACE_CONTEXT);
            return (Node) xpath.evaluate(xpathQuery, metadata, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            throw new RuntimeException("Error evaluating XPath: " + xpathQuery, ex);
        }
    }

    private static String getAttributeValue(Node node, String attributeName) {
        Node namedItem = node.getAttributes().getNamedItem(attributeName);
        if (namedItem != null) {
            return namedItem.getNodeValue();
        } else {
            return null;
        }
    }

    public static class Reference {

        private final String name;
        private final String interfaceType;
        private final ReferenceCardinality cardinality;
        private final String bind;
        private final String unbind;

        public Reference(Node node) {
            this.name = getAttributeValue(node, "name");
            this.interfaceType = getAttributeValue(node, "interface");
            this.cardinality = toCardinality(getAttributeValue(node, "cardinality"));
            this.bind = getAttributeValue(node, "bind");
            this.unbind = getAttributeValue(node, "unbind");
        }

        private ReferenceCardinality toCardinality(String value) {
            for (ReferenceCardinality item : ReferenceCardinality.values()) {
                if (StringUtils.equals(item.getCardinalityString(), value)) {
                    return item;
                }
            }
            return ReferenceCardinality.MANDATORY_UNARY;
        }

        public String getName() {
            return this.name;
        }

        public String getInterfaceType() {
            return this.interfaceType;
        }

        public ReferenceCardinality getCardinality() {
            return this.cardinality;
        }

        public String getBind() {
            return this.bind;
        }

        public String getUnbind() {
            return this.unbind;
        }

    }

}
