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
package org.apache.sling.testing.clients.html;

import org.apache.http.HttpEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.html.microdata.State;

public interface HtmlClient {

    State enter(String url) throws ClientException;

    /**
     * Performs a GET request and returns a State
     *
     * @param path the url as string
     * @return the response as State
     * @throws ClientException if the request could not be completed
     */
    State get(String path) throws ClientException;

    /**
     * Performs a POST request.
     *
     * @param path the url as string
     * @param entity additional POST entity
     * @return the response as State
     * @throws ClientException if the request could not be completed
     */
    State post(String path, HttpEntity entity) throws ClientException;

    /**
     * Performs a DELETE request and returns a State
     *
     * @param path the path as string
     * @return the response as State
     * @throws ClientException if the request could not be completed
     */
    State delete(String path) throws ClientException;

}
