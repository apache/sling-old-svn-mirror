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
package org.apache.sling.hapi.client.impl;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.sling.hapi.client.ClientException;
import org.apache.sling.hapi.client.Document;
import org.apache.sling.hapi.client.HtmlClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class AbstractHtmlClientImpl implements HttpClient, HtmlClient {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractHtmlClientImpl.class);
    protected CloseableHttpClient client;
    protected URI baseUrl;

    public AbstractHtmlClientImpl(CloseableHttpClient client, String baseUrl) throws URISyntaxException {
        this.client = client;
        this.baseUrl = new URI(baseUrl);
    }

    public AbstractHtmlClientImpl(String baseUrl) throws URISyntaxException {
        this(HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build(), baseUrl);
    }

    public AbstractHtmlClientImpl(String baseUrl, String user, String password) throws URISyntaxException {
        this.baseUrl = new URI(baseUrl);
        HttpHost targetHost = URIUtils.extractHost(this.baseUrl);
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new UsernamePasswordCredentials(user, password));
        this.client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credsProvider)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
    }


    @Override
    public <T extends Document> T enter(String url) throws ClientException {
        return get(url);
    }

    @Override
    public <T extends Document> T get(String url) throws ClientException {
        try {
            URI absoluteUri = absoluteUri(url);
            LOG.info("GET " + absoluteUri);
            HttpResponse response = this.execute(new HttpGet(absoluteUri));
            return newDocument(EntityUtils.toString(response.getEntity()));

        } catch (URISyntaxException e) {
            throw new ClientException("Invalid get url " + url, e);
        } catch (Exception e) {
            throw new ClientException("Could not execute GET request", e);
        }
    }

    @Override
    public <T extends Document> T post(String url, HttpEntity entity) throws ClientException {
        try {
            URI absoluteUri = absoluteUri(url);
            LOG.info("POST " + absoluteUri);
            HttpPost post = new HttpPost(absoluteUri);
            post.setEntity(entity);
            HttpResponse response = this.execute(post);
            return newDocument(EntityUtils.toString(response.getEntity()));
        } catch (URISyntaxException e) {
            throw new ClientException("Invalid post url " + url, e);
        } catch (Exception e) {
            throw new ClientException("Could not execute POST request", e);
        }
    }

    @Override
    public <T extends Document> T delete(String url) throws ClientException {
        try {
            URI absoluteUri = absoluteUri(url);
            LOG.info("DELETE " + absoluteUri);
            HttpResponse response = this.execute(new HttpDelete(absoluteUri));
            return newDocument(response.getEntity().toString());
        } catch (URISyntaxException e) {
            throw new ClientException("Invalid post url " + url, e);
        } catch (Exception e) {
            throw new ClientException("Could not execute DELETE request", e);
        }
    }

    @Override
    public abstract <T extends Document> T newDocument(String html);


    private URI absoluteUri(String url) throws URISyntaxException {
        URI getUrl = new URI(url);
        return this.baseUrl.resolve(getUrl);
    }

    /*
        Overrides of HttpClient methods
     */
    @Override
    public HttpParams getParams() {
        return client.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return client.getConnectionManager();
    }

    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException, ClientProtocolException {
        return client.execute(httpUriRequest);
    }

    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
        return client.execute(httpUriRequest, httpContext);
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest) throws IOException, ClientProtocolException {
        return client.execute(httpHost, httpRequest);
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
        return client.execute(httpHost, httpRequest, httpContext);
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return client.execute(httpUriRequest, responseHandler);
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException, ClientProtocolException {
        return client.execute(httpUriRequest, responseHandler, httpContext);
    }

    @Override
    public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return client.execute(httpHost, httpRequest, responseHandler);
    }

    @Override
    public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException, ClientProtocolException {
        return client.execute(httpHost, httpRequest, responseHandler, httpContext);
    }
}
