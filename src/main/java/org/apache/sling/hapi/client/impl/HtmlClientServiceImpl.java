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
package org.apache.sling.hapi.client.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.hapi.client.HtmlClient;
import org.apache.sling.hapi.client.HtmlClientService;
import org.apache.sling.hapi.client.impl.microdata.MicrodataHtmlClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

@Component(metatype = false)
@Service(value = HtmlClientService.class)
public class HtmlClientServiceImpl implements HtmlClientService {

    private final Logger LOG = LoggerFactory.getLogger(HtmlClientService.class);

    /**
     * Get the HtmlClient
     * @param client The inner http client
     * @param baseUrl the base URL as String
     * @return the HtmlClient or null if there was an error
     */
    public HtmlClient getClient(CloseableHttpClient client, String baseUrl) {
        try {
            return new MicrodataHtmlClient(client, baseUrl);
        } catch (URISyntaxException e) {
            LOG.error("Cannot instantiate client", e);
            return null;
        }
    }

    /**
     * {@see HtmlClient}
     * @param baseUrl the base URL as String
     * @return the HtmlClient or null if there was an error
     */
    public HtmlClient getClient(String baseUrl) {
        try {
            return new MicrodataHtmlClient(baseUrl);
        } catch (URISyntaxException e) {
            LOG.error("Cannot instantiate client", e);
            return null;
        }
    }

    /**
     * {@see HtmlClient}
     * @param baseUrl the base URL as String
     * @param user the username to be used for basic auth
     * @param password the password to be used for basic auth
     * @return the HtmlClient or null if there was an error
     */
    public HtmlClient getClient(String baseUrl, String user, String password) {
        try {
            return new MicrodataHtmlClient(baseUrl, user, password);
        } catch (URISyntaxException e) {
            LOG.error("Cannot instantiate client", e);
            return null;
        }
    }
}
