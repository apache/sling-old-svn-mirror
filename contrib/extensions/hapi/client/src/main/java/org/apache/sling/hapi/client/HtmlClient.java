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

import org.apache.http.HttpEntity;

public interface HtmlClient {

    /**
     * Enters a url and return a Document
     *
     * @param url
     * @return
     * @throws ClientException
     */
    <T extends Document> T enter(String url) throws ClientException;

    /**
     * Performs a GET request and returns a Document
     *
     * @param url the URL String to perform an HTTP GET on
     * @return
     * @throws ClientException
     */
    <T extends Document> T get(String url) throws ClientException;


    /**
     * Performs a POST request.
     *
     * @param url    the URL String to perform an HTTP post on
     * @param entity data to post
     * @return
     * @throws ClientException
     */
    <T extends Document> T post(String url, HttpEntity entity) throws ClientException;

    /**
     * Performs a DELETE request and returns a Document
     *
     * @param url the URL String to perform an HTTP DELETE on
     * @return
     * @throws ClientException
     */
    <T extends Document> T delete(String url) throws ClientException;

    /**
     * Method to create a new Document representation from an HTML content String
     * @param html
     * @param <T>
     * @return
     */
    <T extends Document> T newDocument(String html);

}
