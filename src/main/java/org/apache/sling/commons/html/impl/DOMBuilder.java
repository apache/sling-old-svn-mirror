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
package org.apache.sling.commons.html.impl;

import java.io.IOException;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * The <code>DOMBuilder</code> is a utility class that will generate a W3C
 * DOM Document from SAX events.
 *
 */
public class DOMBuilder implements ContentHandler, LexicalHandler {

    /** The default transformer factory shared by all instances */
    private static final SAXTransformerFactory FACTORY = (SAXTransformerFactory) TransformerFactory.newInstance();

    /** The result */
    private final DOMResult result;

    private final ContentHandler contentHandler;
    private final LexicalHandler lexicalHandler;

    /**
     * Construct a new instance of this DOMBuilder.
     */
    public DOMBuilder() throws IOException {
        try {
            final TransformerHandler handler = FACTORY.newTransformerHandler();
            this.contentHandler = handler;
            this.lexicalHandler = handler;
            this.result = new DOMResult();
            handler.setResult(this.result);
        } catch (javax.xml.transform.TransformerException local) {
            throw (IOException) new IOException("Fatal-Error: Unable to get transformer handler").initCause(local);
        }
    }

    /**
     * Return the newly built Document.
     */
    public Document getDocument() {
        if (this.result.getNode() == null) {
            return null;
        } else if (this.result.getNode().getNodeType() == Node.DOCUMENT_NODE) {
            return (Document) this.result.getNode();
        } else {
            return this.result.getNode().getOwnerDocument();
        }
    }

    public void setDocumentLocator(Locator locator) {
        contentHandler.setDocumentLocator(locator);
    }

    public void startDocument()
    throws SAXException {
        contentHandler.startDocument();
    }

    public void endDocument()
    throws SAXException {
        contentHandler.endDocument();
    }

    public void startPrefixMapping(String prefix, String uri)
    throws SAXException {
        contentHandler.startPrefixMapping(prefix, uri);
    }

    public void endPrefixMapping(String prefix)
    throws SAXException {
        contentHandler.endPrefixMapping(prefix);
    }

    public void startElement(String uri, String loc, String raw, Attributes a)
    throws SAXException {
        contentHandler.startElement(uri, loc, raw, a);
    }

    public void endElement(String uri, String loc, String raw)
    throws SAXException {
        contentHandler.endElement(uri, loc, raw);
    }

    public void characters(char c[], int start, int len)
    throws SAXException {
        contentHandler.characters(c, start, len);
    }

    public void ignorableWhitespace(char c[], int start, int len)
    throws SAXException {
        contentHandler.ignorableWhitespace(c, start, len);
    }

    public void processingInstruction(String target, String data)
    throws SAXException {
        contentHandler.processingInstruction(target, data);
    }

    public void skippedEntity(String name)
    throws SAXException {
        contentHandler.skippedEntity(name);
    }

    public void startDTD(String name, String publicId, String systemId)
    throws SAXException {
        lexicalHandler.startDTD(name, publicId, systemId);
    }

    public void endDTD()
    throws SAXException {
        lexicalHandler.endDTD();
    }

    public void startEntity(String name)
    throws SAXException {
        lexicalHandler.startEntity(name);
    }

    public void endEntity(String name)
    throws SAXException {
        lexicalHandler.endEntity(name);
    }

    public void startCDATA()
    throws SAXException {
        lexicalHandler.startCDATA();
    }

    public void endCDATA()
    throws SAXException {
        lexicalHandler.endCDATA();
    }

    public void comment(char ch[], int start, int len)
    throws SAXException {
        lexicalHandler.comment(ch, start, len);
    }
}
