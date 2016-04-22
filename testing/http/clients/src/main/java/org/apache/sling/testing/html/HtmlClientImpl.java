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
package org.apache.sling.testing.html;

import org.apache.http.HttpEntity;
import org.apache.sling.testing.ClientException;
import org.apache.sling.testing.SlingClient;
import org.apache.sling.testing.SlingHttpResponse;
import org.apache.sling.testing.html.microdata.State;
import org.apache.sling.testing.html.microdata.StateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.URI;

/**
 * The test client that understand HTML semantics. This is mainly used for testing using Microdata.
 * TODO: replace by org.apache.sling.hapi.client.microdata.MicrodataHtmlClient
 */
public class HtmlClientImpl extends SlingClient implements HtmlClient {
    private static final Logger log = LoggerFactory.getLogger(HtmlClientImpl.class);

    public HtmlClientImpl(URI baseUrl, String user, String password) throws ClientException {
        super(baseUrl, user, password);
    }

    /**
     * Navigates to the given URL.
     */
    public State enter(String url) throws ClientException {
        return get(url);
    }

    /**
     * Performs a GET request.
     */
    public State get(String path) throws ClientException {
        log.info("GET " + path);
        SlingHttpResponse response = this.doGet(path);
        return new StateImpl(response.getContent(), path, this);
    }

    /**
     * Performs a POST request.
     */
    public State post(String path, HttpEntity entity) throws ClientException {
        log.info("POST " + path);
        SlingHttpResponse response = this.doPost(path, entity);
        return new StateImpl(response.getContent(), path, this);
    }

    /**
     * Performs a GET request.
     */
    public State delete(String url) throws ClientException {
        log.info("GET " + url);
        SlingHttpResponse response = this.doDelete(url, null, null);
        return new StateImpl(response.getContent(), url, this);
    }
}
