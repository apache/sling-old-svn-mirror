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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.SerializerFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Base class for trax based serializers.
 */
public abstract class AbstractTraxSerializerFactory implements SerializerFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /**
     * The trax <code>TransformerFactory</code> used by this serializer.
     */
    private SAXTransformerFactory tfactory;

    private boolean needsNamespacesAsAttributes;

    protected abstract String getOutputFormat();
    protected abstract String getDoctypePublic();
    protected abstract String getDoctypeSystem();

    /**
     * @see org.apache.sling.rewriter.SerializerFactory#createSerializer()
     */
    public Serializer createSerializer() {
        TransformerHandler tHandler = null;
        try {
            tHandler = this.tfactory.newTransformerHandler();
        } catch (TransformerConfigurationException e) {
            logger.error("Unable to create new transformer handler.", e);
        }
        final ContentHandler ch;
        if ( this.needsNamespacesAsAttributes ) {
            final NamespaceAsAttributes nsPipeline = new NamespaceAsAttributes(tHandler, this.logger);
            ch = nsPipeline;
        } else {
            ch = tHandler;
        }
        return new TraxSerializer(tHandler, ch, getOutputFormat(), getDoctypePublic(), getDoctypeSystem());
    }

    protected void activate(final ComponentContext ctx) {
        this.tfactory = (SAXTransformerFactory) TransformerFactory.newInstance();
        tfactory.setErrorListener(new TraxErrorHandler(this.logger));
        // Check if we need namespace as attributes.
        try {
            this.needsNamespacesAsAttributes = this.needsNamespacesAsAttributes();
        } catch (Exception e) {
            this.logger.warn("Cannot know if transformer needs namespaces attributes - assuming NO.", e);
            this.needsNamespacesAsAttributes = false;
        }
    }

    protected void deactivat(final ComponentContext ctx) {
        this.tfactory = null;
    }

    /**
     * Checks if the used Trax implementation correctly handles namespaces set using
     * <code>startPrefixMapping()</code>, but wants them also as 'xmlns:' attributes.
     * <p>
     * The check consists in sending SAX events representing a minimal namespaced document
     * with namespaces defined only with calls to <code>startPrefixMapping</code> (no
     * xmlns:xxx attributes) and check if they are present in the resulting text.
     */
    protected boolean needsNamespacesAsAttributes() throws Exception {
        // Serialize a minimal document to check how namespaces are handled.
        final StringWriter writer = new StringWriter();

        final String uri = "namespaceuri";
        final String prefix = "nsp";
        final String check = "xmlns:" + prefix + "='" + uri + "'";

        final TransformerHandler handler = this.tfactory.newTransformerHandler();
        handler.setResult(new StreamResult(writer));

        // Output a single element
        handler.startDocument();
        handler.startPrefixMapping(prefix, uri);
        handler.startElement(uri, "element", "element", new AttributesImpl());
        handler.endElement(uri, "element", "element");
        handler.endPrefixMapping(prefix);
        handler.endDocument();

        final String text = writer.toString();

        // Check if the namespace is there (replace " by ' to be sure of what we search in)
        return (text.replace('"', '\'').indexOf(check) == -1);
    }

    //--------------------------------------------------------------------------------------------

    /**
     * A pipe that ensures that all namespace prefixes are also present as
     * 'xmlns:' attributes. This used to circumvent Xalan's serialization behaviour
     * which is to ignore namespaces if they're not present as 'xmlns:xxx' attributes.
     */
    public static class NamespaceAsAttributes implements ContentHandler, LexicalHandler {

        /** The URI for xml namespaces */
        private static final String XML_NAMESPACE_URI = "http://www.w3.org/XML/1998/namespace";

        /**
         * The prefixes of startPrefixMapping() declarations for the coming element.
         */
        private List<String> prefixList = new ArrayList<String>();

        /**
         * The URIs of startPrefixMapping() declarations for the coming element.
         */
        private List<String> uriList = new ArrayList<String>();

        /**
         * Maps of URI<->prefix mappings. Used to work around a bug in the Xalan
         * serializer.
         */
        private Map<String, String> uriToPrefixMap = new HashMap<String, String>();
        private Map<String, String> prefixToUriMap = new HashMap<String, String>();

        /**
         * True if there has been some startPrefixMapping() for the coming element.
         */
        private boolean hasMappings = false;

        protected final ContentHandler contentHandler;
        protected final LexicalHandler lexicalHandler;
        protected final Logger logger;

        public NamespaceAsAttributes(final ContentHandler handler, final Logger logger) {
            this.contentHandler = handler;
            this.lexicalHandler = (LexicalHandler)handler;
            this.logger = logger;
        }

        /**
         * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
         */
        public void setDocumentLocator(Locator locator) {
            contentHandler.setDocumentLocator(locator);
        }

        /**
         * Receive notification of character data.
         *
         * @param c The characters from the XML document.
         * @param start The start position in the array.
         * @param len The number of characters to read from the array.
         */
        public void characters(char c[], int start, int len)
        throws SAXException {
            contentHandler.characters(c, start, len);
        }

        /**
         * Receive notification of ignorable whitespace in element content.
         *
         * @param c The characters from the XML document.
         * @param start The start position in the array.
         * @param len The number of characters to read from the array.
         */
        public void ignorableWhitespace(char c[], int start, int len)
        throws SAXException {
            contentHandler.ignorableWhitespace(c, start, len);
        }

        /**
         * Receive notification of a processing instruction.
         *
         * @param target The processing instruction target.
         * @param data The processing instruction data, or null if none was
         *             supplied.
         */
        public void processingInstruction(String target, String data)
        throws SAXException {
            contentHandler.processingInstruction(target, data);
        }

        /**
         * Receive notification of a skipped entity.
         *
         * @param name The name of the skipped entity.  If it is a  parameter
         *             entity, the name will begin with '%'.
         */
        public void skippedEntity(String name)
        throws SAXException {
            contentHandler.skippedEntity(name);
        }

        /**
         * Report the start of DTD declarations, if any.
         *
         * @param name The document type name.
         * @param publicId The declared public identifier for the external DTD
         *                 subset, or null if none was declared.
         * @param systemId The declared system identifier for the external DTD
         *                 subset, or null if none was declared.
         */
        public void startDTD(String name, String publicId, String systemId)
        throws SAXException {
            lexicalHandler.startDTD(name, publicId, systemId);
        }

        /**
         * Report the end of DTD declarations.
         */
        public void endDTD()
        throws SAXException {
            lexicalHandler.endDTD();
        }

        /**
         * Report the beginning of an entity.
         *
         * @param name The name of the entity. If it is a parameter entity, the
         *             name will begin with '%'.
         */
        public void startEntity(String name)
        throws SAXException {
            lexicalHandler.startEntity(name);
        }

        /**
         * Report the end of an entity.
         *
         * @param name The name of the entity that is ending.
         */
        public void endEntity(String name)
        throws SAXException {
            lexicalHandler.endEntity(name);
        }

        /**
         * Report the start of a CDATA section.
         */
        public void startCDATA()
        throws SAXException {
            lexicalHandler.startCDATA();
        }

        /**
         * Report the end of a CDATA section.
         */
        public void endCDATA()
        throws SAXException {
            lexicalHandler.endCDATA();
        }

        /**
         * Report an XML comment anywhere in the document.
         *
         * @param ch An array holding the characters in the comment.
         * @param start The starting position in the array.
         * @param len The number of characters to use from the array.
         */
        public void comment(char ch[], int start, int len)
        throws SAXException {
            lexicalHandler.comment(ch, start, len);
        }

        public void startDocument() throws SAXException {
            // Cleanup
            this.uriToPrefixMap.clear();
            this.prefixToUriMap.clear();
            clearMappings();
            this.contentHandler.startDocument();
        }

        /**
         * Track mappings to be able to add <code>xmlns:</code> attributes
         * in <code>startElement()</code>.
         */
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // Store the mappings to reconstitute xmlns:attributes
            // except prefixes starting with "xml": these are reserved
            // VG: (uri != null) fixes NPE in startElement
            if (uri != null && !prefix.startsWith("xml")) {
                this.hasMappings = true;
                this.prefixList.add(prefix);
                this.uriList.add(uri);

                // append the prefix colon now, in order to save concatenations later, but
                // only for non-empty prefixes.
                if (prefix.length() > 0) {
                    this.uriToPrefixMap.put(uri, prefix + ":");
                } else {
                    this.uriToPrefixMap.put(uri, prefix);
                }

                this.prefixToUriMap.put(prefix, uri);
            }
            this.contentHandler.startPrefixMapping(prefix, uri);
        }

        /**
         * Ensure all namespace declarations are present as <code>xmlns:</code> attributes
         * and add those needed before calling superclass. This is a workaround for a Xalan bug
         * (at least in version 2.0.1) : <code>org.apache.xalan.serialize.SerializerToXML</code>
         * ignores <code>start/endPrefixMapping()</code>.
         */
        public void startElement(String eltUri, String eltLocalName, String eltQName, Attributes attrs)
                throws SAXException {

            // try to restore the qName. The map already contains the colon
            if (null != eltUri && eltUri.length() != 0 && this.uriToPrefixMap.containsKey(eltUri)) {
                eltQName = this.uriToPrefixMap.get(eltUri) + eltLocalName;
            }
            if (this.hasMappings) {
                // Add xmlns* attributes where needed

                // New Attributes if we have to add some.
                AttributesImpl newAttrs = null;

                int mappingCount = this.prefixList.size();
                int attrCount = attrs.getLength();

                for (int mapping = 0; mapping < mappingCount; mapping++) {

                    // Build infos for this namespace
                    String uri = this.uriList.get(mapping);
                    String prefix = this.prefixList.get(mapping);
                    String qName = prefix.equals("") ? "xmlns" : ("xmlns:" + prefix);

                    // Search for the corresponding xmlns* attribute
                    boolean found = false;
                    for (int attr = 0; attr < attrCount; attr++) {
                        if (qName.equals(attrs.getQName(attr))) {
                            // Check if mapping and attribute URI match
                            if (!uri.equals(attrs.getValue(attr))) {
                                logger.error("URI in prefix mapping and attribute do not match : '"
                                                  + uri + "' - '" + attrs.getURI(attr) + "'");
                                throw new SAXException("URI in prefix mapping and attribute do not match");
                            }
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // Need to add this namespace
                        if (newAttrs == null) {
                            // Need to test if attrs is empty or we go into an infinite loop...
                            // Well know SAX bug which I spent 3 hours to remind of :-(
                            if (attrCount == 0) {
                                newAttrs = new AttributesImpl();
                            } else {
                                newAttrs = new AttributesImpl(attrs);
                            }
                        }

                        if (prefix.equals("")) {
                            newAttrs.addAttribute(XML_NAMESPACE_URI, "xmlns", "xmlns", "CDATA", uri);
                        } else {
                            newAttrs.addAttribute(XML_NAMESPACE_URI, prefix, qName, "CDATA", uri);
                        }
                    }
                } // end for mapping

                // Cleanup for the next element
                clearMappings();

                // Start element with new attributes, if any
                this.contentHandler.startElement(eltUri, eltLocalName, eltQName, newAttrs == null ? attrs : newAttrs);
            } else {
                // Normal job
                this.contentHandler.startElement(eltUri, eltLocalName, eltQName, attrs);
            }
        }


        /**
         * Receive notification of the end of an element.
         * Try to restore the element qName.
         */
        public void endElement(String eltUri, String eltLocalName, String eltQName) throws SAXException {
            // try to restore the qName. The map already contains the colon
            if (null != eltUri && eltUri.length() != 0 && this.uriToPrefixMap.containsKey(eltUri)) {
                eltQName = this.uriToPrefixMap.get(eltUri) + eltLocalName;
            }
            this.contentHandler.endElement(eltUri, eltLocalName, eltQName);
        }

        /**
         * End the scope of a prefix-URI mapping:
         * remove entry from mapping tables.
         */
        public void endPrefixMapping(String prefix) throws SAXException {
            // remove mappings for xalan-bug-workaround.
            // Unfortunately, we're not passed the uri, but the prefix here,
            // so we need to maintain maps in both directions.
            if (this.prefixToUriMap.containsKey(prefix)) {
                this.uriToPrefixMap.remove(this.prefixToUriMap.get(prefix));
                this.prefixToUriMap.remove(prefix);
            }

            if (hasMappings) {
                // most of the time, start/endPrefixMapping calls have an element event between them,
                // which will clear the hasMapping flag and so this code will only be executed in the
                // rather rare occasion when there are start/endPrefixMapping calls with no element
                // event in between. If we wouldn't remove the items from the prefixList and uriList here,
                // the namespace would be incorrectly declared on the next element following the
                // endPrefixMapping call.
                int pos = prefixList.lastIndexOf(prefix);
                if (pos != -1) {
                    prefixList.remove(pos);
                    uriList.remove(pos);
                }
            }

            this.contentHandler.endPrefixMapping(prefix);
        }

        /**
         *
         */
        public void endDocument() throws SAXException {
            // Cleanup
            this.uriToPrefixMap.clear();
            this.prefixToUriMap.clear();
            clearMappings();
            this.contentHandler.endDocument();
        }

        private void clearMappings() {
            this.hasMappings = false;
            this.prefixList.clear();
            this.uriList.clear();
        }
    }
}
