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

package org.apache.sling.hapi.client.test;

import static org.hamcrest.core.IsEqual.equalTo;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.sling.hapi.client.ClientException;
import org.apache.sling.hapi.client.Document;
import org.apache.sling.hapi.client.Items;
import org.apache.sling.hapi.client.microdata.MicrodataHtmlClient;
import org.apache.sling.hapi.client.test.util.HttpServerRule;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class ItemsTest {
    private static final String GET_URL = "/test";
    private static final String GET_LINKS_URL = "/testlinks";

    @ClassRule
    public static final HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() throws IOException {
            final String html = IOUtils.toString(ItemsTest.class.getResourceAsStream("/items.html"), "UTF-8");
            final String htmlLinks = IOUtils.toString(ItemsTest.class.getResourceAsStream("/items_links.html"), "UTF-8");
            serverBootstrap.registerHandler(GET_URL, new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext)
                        throws HttpException, IOException {
                    HttpEntity entity = new StringEntity(html, "UTF-8");
                    httpResponse.setEntity(entity);
                }
            }).registerHandler(GET_LINKS_URL, new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext)
                        throws HttpException, IOException {
                    HttpEntity entity = new StringEntity(htmlLinks, "UTF-8");
                    httpResponse.setEntity(entity);
                }
            });
        }
    };
    
    @Test
    public void testItems() throws ClientException, URISyntaxException {
        MicrodataHtmlClient client = new MicrodataHtmlClient(httpServer.getURI().toString());
        Document doc = client.enter(GET_URL);
        Items items = doc.items();
        Assert.assertThat(items.length(), equalTo(2));
        for (int i=0; i<2; i++) {
            Assert.assertThat(items.at(i).prop("name").text(), equalTo("Avatar" + i));
            Assert.assertThat(items.at(i).prop("genre").text(), equalTo("Science fiction" + i));
            Assert.assertThat(items.at(i).prop("rank").number(), equalTo(i));
            Assert.assertThat(items.at(i).prop("director").prop("name").text(), equalTo("James Cameron" + i));
            Assert.assertThat(items.at(i).prop("director").prop("birthDate").text(), equalTo("August 16, 1954 - " + i));
        }
    }

    @Test
    public void testItemsLinks() throws ClientException, URISyntaxException {
        MicrodataHtmlClient client = new MicrodataHtmlClient(httpServer.getURI().toString());
        Document doc = client.enter(GET_LINKS_URL);
        Items items = doc.items();
        Assert.assertThat(items.length(), equalTo(1));
        Assert.assertThat(items.prop("name").text(), equalTo("Avatar"));
        Assert.assertThat(items.prop("genre").text(), equalTo("Science fiction"));
        Assert.assertThat(items.prop("rank").number(), equalTo(2));
        Assert.assertThat(items.prop("director").prop("name").text(), equalTo("James Cameron"));
        Assert.assertThat(items.prop("director").prop("birthDate").text(), equalTo("August 16, 1954"));

        Assert.assertThat(doc.link("test").length(), equalTo(2));
        Assert.assertThat(doc.items().link("test").length(), equalTo(1));
        Assert.assertThat(doc.items().prop("director").link("test").length(), equalTo(1));

        Items otherMovies = doc.link("test").follow().items();
        Assert.assertThat(otherMovies.length(), equalTo(2));

    }
}