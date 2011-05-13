/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.rewriter;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * A generic base transformer which simply delegates all ContentHandler method
 * invocations to the next ContentHandler.
 */
public class DefaultTransformer implements Transformer {
    
    private ContentHandler contentHandler;
    protected ProcessingComponentConfiguration config;
    protected Locator documentLocator;
    protected ProcessingContext processingContext;
    
    public void characters(char[] ac, int i, int j) throws SAXException {
        contentHandler.characters(ac, i, j);
    }

    public void dispose() {
    }


    public void endDocument() throws SAXException {
        contentHandler.endDocument();
    }


    public void endElement(String s, String s1, String s2) throws SAXException {
        contentHandler.endElement(s, s1, s2);
    }


    public void endPrefixMapping(String s) throws SAXException {
        contentHandler.endPrefixMapping(s);
    }


    public void ignorableWhitespace(char[] ac, int i, int j) throws SAXException {
        contentHandler.ignorableWhitespace(ac, i, j);
    }


    public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
        this.processingContext = context;
        this.config = config;
    }


    public void processingInstruction(String s, String s1) throws SAXException {
        contentHandler.processingInstruction(s, s1);
    }


    public final void setContentHandler(ContentHandler handler) {
        this.contentHandler = handler;
    }


    public void setDocumentLocator(Locator locator) {
        this.documentLocator = locator;
        
    }


    public void skippedEntity(String s) throws SAXException {
        contentHandler.skippedEntity(s);
    }


    public void startDocument() throws SAXException {
        contentHandler.startDocument();
    }


    public void startElement(String s, String s1, String s2, Attributes attributes) throws SAXException {
        contentHandler.startElement(s, s1, s2, attributes);
    }

    public void startPrefixMapping(String s, String s1) throws SAXException {
        contentHandler.startPrefixMapping(s, s1);
    }

    protected ContentHandler getContentHandler() {
        return contentHandler;
    }

}
