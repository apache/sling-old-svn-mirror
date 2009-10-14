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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Serializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * The <code>TraxSerializer</code> is a serializer based on
 * the jaxp serializers.
 */
public class TraxSerializer implements Serializer, LexicalHandler {

    /** The default encoding. */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /** The <code>Properties</code> used by this serializer. */
    private final Properties format = new Properties();

    private final TransformerHandler transformerHandler;

    private final ContentHandler contentHandler;
    private final LexicalHandler lexicalHandler;

    public TraxSerializer(final TransformerHandler transformerHandler,
                          final ContentHandler handler,
                          final String outputFormat,
                          final String doctypePublic,
                          final String doctypeSystem) {
        this.contentHandler = handler;
        this.lexicalHandler = (LexicalHandler)handler;
        this.transformerHandler = transformerHandler;
        this.format.put(OutputKeys.METHOD, outputFormat);
        this.format.put(OutputKeys.DOCTYPE_PUBLIC, doctypePublic);
        this.format.put(OutputKeys.DOCTYPE_SYSTEM, doctypeSystem);
    }

    /**
     * @see org.apache.sling.rewriter.Serializer#init(org.apache.sling.rewriter.ProcessingContext, org.apache.sling.rewriter.ProcessingComponentConfiguration)
     */
    public void init(ProcessingContext context,
                     ProcessingComponentConfiguration config)
    throws IOException {
        if ( this.transformerHandler == null ) {
            throw new IOException("Transformer handler could not be instantiated.");
        }
        if ( context.getResponse().getCharacterEncoding() != null ) {
            this.format.put(OutputKeys.ENCODING, context.getResponse().getCharacterEncoding());
        } else {
            this.format.put(OutputKeys.ENCODING, DEFAULT_ENCODING);
        }

        final String cdataSectionElements = config.getConfiguration().get("cdata-section-elements", String.class);
        final String dtPublic = config.getConfiguration().get("doctype-public", String.class);
        final String dtSystem = config.getConfiguration().get("doctype-system", String.class);
        final String encoding = config.getConfiguration().get("encoding", String.class);
        final String indent = config.getConfiguration().get("indent", String.class);
        final String mediaType = config.getConfiguration().get("media-type", String.class);
        final String method = config.getConfiguration().get("method", String.class);
        final String omitXMLDeclaration = config.getConfiguration().get("omit-xml-declaration", String.class);
        final String standAlone = config.getConfiguration().get("standalone", String.class);
        final String version = config.getConfiguration().get("version", String.class);

        if (cdataSectionElements != null) {
            format.put(OutputKeys.CDATA_SECTION_ELEMENTS, cdataSectionElements);
        }
        if (dtPublic != null) {
            format.put(OutputKeys.DOCTYPE_PUBLIC, dtPublic);
        }
        if (dtSystem != null) {
            format.put(OutputKeys.DOCTYPE_SYSTEM, dtSystem);
        }
        if (encoding != null) {
            format.put(OutputKeys.ENCODING, encoding);
        }
        if (indent != null) {
            format.put(OutputKeys.INDENT, indent);
        }
        if (mediaType != null) {
            format.put(OutputKeys.MEDIA_TYPE, mediaType);
        }
        if (method != null) {
            format.put(OutputKeys.METHOD, method);
        }
        if (omitXMLDeclaration != null) {
            format.put(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration);
        }
        if (standAlone != null) {
            format.put(OutputKeys.STANDALONE, standAlone);
        }
        if (version != null) {
            format.put(OutputKeys.VERSION, version);
        }
        this.setOutputStream(context.getOutputStream());
    }

    /**
     * Set the {@link OutputStream} where the requested resource should
     * be serialized.
     */
    private void setOutputStream(OutputStream out) throws IOException {
        try {
            this.transformerHandler.getTransformer().setOutputProperties(this.format);
            this.transformerHandler.setResult(new StreamResult(out));
        } catch (Exception e) {
            final String message = "Cannot set XMLSerializer outputstream";
            throw ((IOException)new IOException(message).initCause(e));
        }
    }

    /**
     * @see org.apache.sling.rewriter.Serializer#dispose()
     */
    public void dispose() {
        // nothing to do
    }

    /**
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator(Locator locator) {
        contentHandler.setDocumentLocator(locator);
    }

    /**
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument()
    throws SAXException {
        contentHandler.startDocument();
    }

    /**
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument()
    throws SAXException {
        contentHandler.endDocument();
    }

    /**
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    public void startPrefixMapping(String prefix, String uri)
    throws SAXException {
        contentHandler.startPrefixMapping(prefix, uri);
    }

    /**
     * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
     */
    public void endPrefixMapping(String prefix)
    throws SAXException {
        contentHandler.endPrefixMapping(prefix);
    }

    public void startElement(String uri, String loc, String raw, Attributes a)
    throws SAXException {
        contentHandler.startElement(uri, loc, raw, a);
    }

    /**
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String loc, String raw)
    throws SAXException {
        contentHandler.endElement(uri, loc, raw);
    }

    /**
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char c[], int start, int len)
    throws SAXException {
        contentHandler.characters(c, start, len);
    }

    /**
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace(char c[], int start, int len)
    throws SAXException {
        contentHandler.ignorableWhitespace(c, start, len);
    }

    /**
     * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction(String target, String data)
    throws SAXException {
        contentHandler.processingInstruction(target, data);
    }

    /**
     * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
     */
    public void skippedEntity(String name)
    throws SAXException {
        contentHandler.skippedEntity(name);
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#startDTD(java.lang.String, java.lang.String, java.lang.String)
     */
    public void startDTD(String name, String publicId, String systemId)
    throws SAXException {
        lexicalHandler.startDTD(name, publicId, systemId);
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#endDTD()
     */
    public void endDTD()
    throws SAXException {
        lexicalHandler.endDTD();
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#startEntity(java.lang.String)
     */
    public void startEntity(String name)
    throws SAXException {
        lexicalHandler.startEntity(name);
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#endEntity(java.lang.String)
     */
    public void endEntity(String name)
    throws SAXException {
        lexicalHandler.endEntity(name);
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#startCDATA()
     */
    public void startCDATA()
    throws SAXException {
        lexicalHandler.startCDATA();
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#endCDATA()
     */
    public void endCDATA()
    throws SAXException {
        lexicalHandler.endCDATA();
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#comment(char[], int, int)
     */
    public void comment(char ch[], int start, int len)
    throws SAXException {
        lexicalHandler.comment(ch, start, len);
    }
}
