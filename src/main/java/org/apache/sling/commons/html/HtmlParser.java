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
package org.apache.sling.commons.html;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * The html parser is a service to parse html and generate
 * SAX events or a Document out of the html.
 *
 */
public interface HtmlParser {

    /**
     * Parse html and send sax events.
     * @param stream The input stream
     * @param encoding Encoding of the input stream, <code>null</code>for default encoding.
     * @param ch Content handler receiving the SAX events. The content handler might also
     *           implement the lexical handler interface.
     */
    void parse(InputStream stream, String encoding, ContentHandler ch) throws SAXException;

    /**
     * Parse html and return a DOM Document.
     * @param The system id
     * @param stream The input stream
     * @param encoding Encoding of the input stream, <code>null</code>for default encoding.
     */
    Document parse(String systemId, InputStream stream, String encoding) throws IOException;
}
