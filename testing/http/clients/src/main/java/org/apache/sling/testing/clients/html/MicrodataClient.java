/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.testing.clients.html;

import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.hapi.client.HtmlClient;
import org.apache.sling.hapi.client.impl.microdata.MicrodataDocument;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MicrodataClient extends SlingClient implements HtmlClient {
    protected static final Logger LOG = LoggerFactory.getLogger(MicrodataClient.class);

    public MicrodataClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
    }

    public MicrodataClient(URI url, String user, String password) throws ClientException {
        super(url, user, password);
    }

    @Override
    public MicrodataDocument enter(String url) throws org.apache.sling.hapi.client.ClientException {
        return get(url);
    }

    @Override
    public MicrodataDocument get(String url) throws org.apache.sling.hapi.client.ClientException {
        try {
            return newDocument(doGet(url).getContent());
        } catch (ClientException e) {
            throw new org.apache.sling.hapi.client.ClientException("Cannot create Microdata document", e);
        }
    }

    @Override
    public MicrodataDocument post(String url, HttpEntity entity) throws org.apache.sling.hapi.client.ClientException {
        try {
            return newDocument(doPost(url, entity).getContent());
        } catch (ClientException e) {
            throw new org.apache.sling.hapi.client.ClientException("Cannot create Microdata document", e);
        }
    }

    @Override
    public MicrodataDocument delete(String url) throws org.apache.sling.hapi.client.ClientException {
        try {
            return newDocument(doDelete(url, null, null).getContent());
        } catch (ClientException e) {
            throw new org.apache.sling.hapi.client.ClientException("Cannot create Microdata document", e);
        }
    }

    @Override
    public MicrodataDocument newDocument(String html) {
        return new MicrodataDocument(html, this, this.getUrl().toString());
    }

}
