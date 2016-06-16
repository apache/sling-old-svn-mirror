/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.html.dom;

import java.io.IOException;


/**
 * Invoked by the <code>HTMLParser</code> when elements are scanned.
 */
public interface DocumentHandler {

    /**
     * Receive notification of unparsed character data.
     *
     * @param ch the character buffer
     * @param off the offset
     * @param len the length of the unparsed character data
     * @throws IOException if the characters cannot be processed
     */
    void onCharacters(char[] ch, int off, int len) throws IOException;

    void onComment(String characters) throws IOException;

    /**
     * Receive notification of the beginning of an element.
     * @param name     tag name
     * @param attList  attribute list
     * @param endSlash flag indicating whether the element is closed with
     *                 an ending slash (xhtml-compliant)
     * @throws IOException if the element cannot be processed
     */
    void onStartElement(String name, AttributeList attList, boolean endSlash)
    throws IOException;

    /**
     * Receive notification of the end of an element.
     * @param name tag name
     * @throws IOException if the element cannot be processed
     */
    void onEndElement(String name)
    throws IOException;

    /**
     * Receive notification of parsing start.
     *
     * @throws IOException if the parsing operation cannot start
     */
    void onStart() throws IOException;

    /**
     * Receive notification of parsing end.
     *
     * @throws IOException if the parsing operation cannot end
     */
    void onEnd() throws IOException;
}
