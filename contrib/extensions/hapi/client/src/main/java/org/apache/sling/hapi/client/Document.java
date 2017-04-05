/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.client;

/**
 * A hapi representation of an HTML document, backed by HTML markup.
 * The Document provides a structure, accessible through the {@link #item(String)} and {@link #items()} methods
 * and a way to use the hypermedia controls through the {@link #link(String)} and {@link #form(String)} methods
 */
public interface Document {
    /**
     * Get all the {@link Document}'s link items. These Items should normally be backed by HTML <i>anchors</i> and <i>links</i>.
     * @param rel An identifier that groups all the <i>link</i> Items for this Document
     * @return all the link Items for this Document, that have the given relation
     * @throws ClientException
     */
    Items link(String rel) throws ClientException;

    /**
     * Get all the {@link Document}'s form items. These Items should normally be backed by the HTML <i>form</i> element
     * @param rel An identifier that groups all the <i>form</i> Items for this Document
     * @return all the form Items for this Document, that have the given relation
     * @throws ClientException
     */
    Items form(String rel) throws ClientException;

    /**
     * Get all the {@link Document}'s items. These Items are backed by any HTML element
     * @param rel An identifier that groups all the Items for this Document
     * @return all the Items for this Document, that have the given relation
     * @throws ClientException
     */
    Items item(String rel) throws ClientException;

    /**
     * Get all the {@link Document}'s items. These Items are backed by any HTML element
     * @return all the Items for this Document
     * @throws ClientException
     */
    Items items() throws ClientException;
}
