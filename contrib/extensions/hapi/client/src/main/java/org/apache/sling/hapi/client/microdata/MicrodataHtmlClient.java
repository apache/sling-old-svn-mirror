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
package org.apache.sling.hapi.client.microdata;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.hapi.client.impl.AbstractHtmlClientImpl;

import java.net.URISyntaxException;

public class MicrodataHtmlClient extends AbstractHtmlClientImpl {

    public MicrodataHtmlClient(CloseableHttpClient client, String baseUrl) throws URISyntaxException {
        super(client, baseUrl);
    }

    public MicrodataHtmlClient(String baseUrl) throws URISyntaxException {
        super(baseUrl);
    }

    public MicrodataHtmlClient(String baseUrl, String user, String password) throws URISyntaxException {
        super(baseUrl, user, password);
    }

    @Override
    public MicrodataDocument newDocument(String html) {
        return new MicrodataDocument(html, this, this.baseUrl.toString());
    }
}
