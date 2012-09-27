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
package org.apache.sling.commons.html.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.html.HtmlParser;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

@Component
@Service(value=HtmlParser.class)
public class HtmlParserImpl implements HtmlParser {

    /**
     * @see org.apache.sling.commons.html.HtmlParser#parse(java.io.InputStream, java.lang.String, org.xml.sax.ContentHandler)
     */
    public void parse(InputStream stream, String encoding, ContentHandler ch)
    throws SAXException {
        final Parser parser = new Parser();
        if ( ch instanceof LexicalHandler ) {
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", ch);
        }
        parser.setContentHandler(ch);
        final InputSource source = new InputSource(stream);
        source.setEncoding(encoding);
        try {
            parser.parse(source);
        } catch (IOException ioe) {
            throw new SAXException(ioe);
        }
    }

    /**
     * @see org.apache.sling.commons.html.HtmlParser#parse(java.lang.String, java.io.InputStream, java.lang.String)
     */
    public Document parse(String systemId, InputStream stream, String encoding) throws IOException {
        final Parser parser = new Parser();

        final DOMBuilder builder = new DOMBuilder();

        final InputSource source = new InputSource(stream);
        source.setEncoding(encoding);
        source.setSystemId(systemId);

        try {
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", builder);
            parser.setContentHandler(builder);
            parser.parse(source);
        } catch (SAXException se) {
            if ( se.getCause() instanceof IOException ) {
                throw (IOException) se.getCause();
            }
            throw (IOException) new IOException("Unable to parse xml.").initCause(se);
        }
        return builder.getDocument();
    }
}
