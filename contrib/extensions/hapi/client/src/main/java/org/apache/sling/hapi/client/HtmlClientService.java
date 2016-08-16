/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.client;

import org.apache.http.impl.client.CloseableHttpClient;

public interface HtmlClientService {

    /**
     * Get an HtmlClient that internally uses a CloseableHttpClient
     * @param client the inner {@link CloseableHttpClient}. The client should take care of any timeouts, authentication, pre/post
     *               processing, etc.
     * @param baseUrl The address prefix to all the http requests (e.g. http://localhost:8080/myapp/)
     * @return
     */
    HtmlClient getClient(CloseableHttpClient client, String baseUrl);

    /**
     * Get an HtmlClient.
     * @param baseUrl The address prefix to all the http requests (e.g. http://localhost:8080/myapp/)
     * @return
     */
    HtmlClient getClient(String baseUrl);

    /**
     * Get an HtmlClient that uses BasicAuth for all requests
     * @param baseUrl The address prefix to all the http requests (e.g. http://localhost:8080/myapp/)
     * @param user The username for BasicAuth
     * @param password The password for BasicAuth
     * @return
     */
    HtmlClient getClient(String baseUrl, String user, String password);
}