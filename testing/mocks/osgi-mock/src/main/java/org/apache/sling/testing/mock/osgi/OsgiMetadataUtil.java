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
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;

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

    private static final OsgiMetadata NULL_METADATA = new OsgiMetadata();

    /*
     * The OSGI metadata XML files do not change during the unit test runs because static part of classpath.
     * So we can cache the parsing step if we need them multiple times.
     */
    private static final LoadingCache<Class, OsgiMetadata> METADATA_CACHE = CacheBuilder.newBuilder().build(new CacheLoader<Class, OsgiMetadata>() {
        @Override
        public OsgiMetadata load(Class clazz) throws Exception {
            List<Document> metadataDocuments = OsgiMetadataUtil.getMetadataDocument(clazz);
            if (metadataDocuments != null) {
                for (Document metadataDocument : metadataDocuments) {
                    if (matchesService(clazz, metadataDocument)) {
                        return new OsgiMetadata(clazz, metadataDocument);
                    }
                }
            }
            return NULL_METADATA;
        }
    });

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
        return "OSGI-INF/" + StringUtils.substringBefore(clazz.getName(), "$") + ".xml";
    }

    public static String getOldMetadataMultiPath() {
        return "OSGI-INF/serviceComponents.xml";
    }

    /**
     * Try to read OSGI-metadata from /OSGI-INF and read all implemented interfaces and service properties.
     * The metadata is cached after initial read, so it's no problem to call this method multiple time for the same class.
     * @param clazz OSGi service implementation class
     * @return Metadata object or null if no metadata present in classpath
     */
    public static OsgiMetadata getMetadata(Class clazz) {
        try {
            OsgiMetadata metadata = METADATA_CACHE.get(clazz);
            if (metadata == NULL_METADATA) {
                return null;
            }
            else {
                return metadata;
            }
        }
        catch (ExecutionException ex) {
            throw new RuntimeException("Error loading OSGi metadata from loader cache.", ex);
        }
    }

    private static List<Document> getMetadataDocument(Class clazz) {
        String metadataPath = getMetadataPath(clazz);
        InputStream metadataStream = OsgiMetadataUtil.class.getClassLoader().getResourceAsStream(metadataPath);
        if (metadataStream == null) {
            String oldMetadataPath = getOldMetadataMultiPath();
            log.debug("No OSGi metadata found at {}, try to fallback to {}", metadataPath, oldMetadataPath);

            try {
                Enumeration<URL> metadataUrls = OsgiMetadataUtil.class.getClassLoader().getResources(oldMetadataPath);
                List<Document> docs = new ArrayList<Document>();
                while (metadataUrls.hasMoreElements()) {
                    URL metadataUrl = metadataUrls.nextElement();
                    metadataStream = metadataUrl.openStream();
                    docs.add(toXmlDocument(metadataStream, oldMetadataPath));
                }
                if (docs.size() == 0) {
                    return null;
                }
                else {
                    return docs;
                }
            }
            catch (IOException ex) {
                throw new RuntimeException("Unable to read classpath resource: " + oldMetadataPath, ex);
            }
        }
        else {
            return ImmutableList.of(toXmlDocument(metadataStream, metadataPath));
        }
    }

    private static Document toXmlDocument(InputStream inputStream, String path) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(inputStream);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("Unable to read classpath resource: " + path, ex);
        } catch (SAXException ex) {
            throw new RuntimeException("Unable to read classpath resource: " + path, ex);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read classpath resource: " + path, ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }
    
    /**
     * @param clazz OSGi component
     * @return XPath query fragment to find matching XML node in SCR metadata
     */
    private static String getComponentXPathQuery(Class clazz) {
        String className = StringUtils.substringBefore(clazz.getName(), "$$Enhancer");
        return "//*[implementation/@class='" + className + "' or @name='" + className + "']";
    }

    private static boolean matchesService(Class clazz, Document metadata) {
        String query = getComponentXPathQuery(clazz);
        NodeList nodes = queryNodes(metadata, query);
        return nodes != null && nodes.getLength() > 0;
    }

    private static Set<String> getServiceInterfaces(Class clazz, Document metadata) {
        Set<String> serviceInterfaces = new HashSet<String>();
        String query = getComponentXPathQuery(clazz) + "/service/provide[@interface!='']";
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

    private static Map<String, Object> getProperties(Class clazz, Document metadata) {
        Map<String, Object> props = new HashMap<String, Object>();
        String query = getComponentXPathQuery(clazz) + "/property[@name!='' and @value!='']";
        NodeList nodes = queryNodes(metadata, query);
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String name = getAttributeValue(node, "name");
                String value = getAttributeValue(node, "value");
                String type = getAttributeValue(node, "type");
                if (StringUtils.equals("Integer", type)) {
                    props.put(name, Integer.parseInt(value));
                }
                else if (StringUtils.equals("Long", type)) {
                    props.put(name, Long.parseLong(value));
                }
                else if (StringUtils.equals("Boolean", type)) {
                    props.put(name, Boolean.parseBoolean(value));
                }
                else {
                    props.put(name, value);
                }
            }
        }
        query = getComponentXPathQuery(clazz) + "/property[@name!='' and text()!='']";
        nodes = queryNodes(metadata, query);
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String name = getAttributeValue(node, "name");
                String[] value = StringUtils.split(StringUtils.trim(node.getTextContent()), "\n\r");
                for (int j = 0; j<value.length; j++) {
                    value[j] = StringUtils.trim(value[j]);
                }
                props.put(name, value);
            }
        }
        return props;
    }

    private static List<Reference> getReferences(Class clazz, Document metadata) {
        List<Reference> references = new ArrayList<Reference>();
        String query = getComponentXPathQuery(clazz) + "/reference[@name!='']";
        NodeList nodes = queryNodes(metadata, query);
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                references.add(new Reference(clazz, node));
            }
        }
        return references;
    }

    private static String getLifecycleMethodName(Class clazz, Document metadata, String methodName) {
        String query = getComponentXPathQuery(clazz);
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

    static class OsgiMetadata {

        private final Class<?> clazz;
        private final Set<String> serviceInterfaces;
        private final Map<String, Object> properties;
        private final List<Reference> references;
        private final String activateMethodName;
        private final String deactivateMethodName;
        private final String modifiedMethodName;

        private OsgiMetadata(Class<?> clazz, Document metadataDocument) {
            this.clazz = clazz;
            this.serviceInterfaces = OsgiMetadataUtil.getServiceInterfaces(clazz, metadataDocument);
            this.properties = OsgiMetadataUtil.getProperties(clazz, metadataDocument);
            this.references = OsgiMetadataUtil.getReferences(clazz, metadataDocument);
            this.activateMethodName = OsgiMetadataUtil.getLifecycleMethodName(clazz, metadataDocument, "activate");
            this.deactivateMethodName = OsgiMetadataUtil.getLifecycleMethodName(clazz, metadataDocument, "deactivate");
            this.modifiedMethodName = OsgiMetadataUtil.getLifecycleMethodName(clazz, metadataDocument, "modified");
        }

        private OsgiMetadata() {
            this.clazz = null;
            this.serviceInterfaces = null;
            this.properties = null;
            this.references = null;
            this.activateMethodName = null;
            this.deactivateMethodName = null;
            this.modifiedMethodName = null;
        }

        public Class<?> getServiceClass() {
            return clazz;
        }

        public Set<String> getServiceInterfaces() {
            return serviceInterfaces;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public List<Reference> getReferences() {
            return references;
        }

        public String getActivateMethodName() {
            return activateMethodName;
        }

        public String getDeactivateMethodName() {
            return deactivateMethodName;
        }

        public String getModifiedMethodName() {
            return modifiedMethodName;
        }

    }

    static class Reference {

        private final Class<?> clazz;
        private final String name;
        private final String interfaceType;
        private final ReferenceCardinality cardinality;
        private final ReferencePolicy policy;
        private final String bind;
        private final String unbind;

        private Reference(Class<?> clazz, Node node) {
            this.clazz = clazz;
            this.name = getAttributeValue(node, "name");
            this.interfaceType = getAttributeValue(node, "interface");
            this.cardinality = toCardinality(getAttributeValue(node, "cardinality"));
            this.policy = toPolicy(getAttributeValue(node, "policy"));
            this.bind = getAttributeValue(node, "bind");
            this.unbind = getAttributeValue(node, "unbind");
        }

        public Class<?> getServiceClass() {
            return clazz;
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

        public ReferencePolicy getPolicy() {
            return policy;
        }

        public String getBind() {
            return this.bind;
        }

        public String getUnbind() {
            return this.unbind;
        }

        private static ReferenceCardinality toCardinality(String value) {
            for (ReferenceCardinality item : ReferenceCardinality.values()) {
                if (StringUtils.equals(item.getCardinalityString(), value)) {
                    return item;
                }
            }
            return ReferenceCardinality.MANDATORY_UNARY;
        }

        private static ReferencePolicy toPolicy(String value) {
            for (ReferencePolicy item : ReferencePolicy.values()) {
                if (StringUtils.equalsIgnoreCase(item.name(), value)) {
                    return item;
                }
            }
            return ReferencePolicy.STATIC;
        }

    }

}
