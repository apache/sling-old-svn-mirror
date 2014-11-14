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

package org.apache.sling.scripting.sightly.compiler.api;

/**
 * Interface used to communicate between HTML parsers and the compiler
 * front-end.
 */
public interface MarkupHandler {

    /**
     * Signal the start of a new element
     * @param markup - the markup of the open tag (Something like \< div)
     * @param tagName - the tag of the element
     */
    void onOpenTagStart(String markup, String tagName);

    /**
     * Signal a element attribute
     * @param name - the name of the attribute
     * @param value - the value of the attribute. If the attribute has no value, null should be passed
     */
    void onAttribute(String name, String value);

    /**
     * Signal that the start tag has ended
     * @param markup the markup for this event
     */
    void onOpenTagEnd(String markup);

    /**
     * @param markup the HTML markup for this event
     * Signal that a tag was closed
     */
    void onCloseTag(String markup);

    /**
     * Signal a text node
     * @param text the raw text content
     */
    void onText(String text);

    /**
     * Signal a comment node
     * @param markup - the markup for the comment
     */
    void onComment(String markup);

    /**
     * Signal a data node
     * @param markup - the markup for this event
     */
    void onDataNode(String markup);

    /**
     * Signal a document type declaration
     * @param markup - the markup for this event
     */
    void onDocType(String markup);

    /**
     * Signal that the document was fully processed
     */
    void onDocumentFinished();
}
