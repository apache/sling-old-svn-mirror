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

import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.sling.hapi.client.ClientException;
import org.apache.sling.hapi.client.Document;
import org.apache.sling.hapi.client.Items;
import org.apache.sling.hapi.client.microdata.MicrodataHtmlClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import util.TestBase;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.core.IsEqual.equalTo;


public class ItemsTest extends TestBase {
    public static final String GET_URL = "/test";
    public static final String GET_LINKS_URL = "/testlinks";
    public static final String OK_RESPONSE = "TEST_OK";
    public static final String FAIL_RESPONSE = "TEST_FAIL";

    public static String html;
    public static String htmlLinks;

    private static HttpHost host;
    private static URI uri;

    @BeforeClass
    public static void setUp() throws Exception {
        ItemsTest.html = IOUtils.toString(ItemsTest.class.getResourceAsStream("items.html"), "UTF-8");
        ItemsTest.htmlLinks = IOUtils.toString(ItemsTest.class.getResourceAsStream("items_links.html"), "UTF-8");
        setupServer();
    }

    public static void setupServer() throws Exception {
        TestBase.setUp();
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

        // start server
        host = TestBase.start();
        uri = URIUtils.rewriteURI(new URI("/"), host);
    }

    @Test
    public void testItems() throws ClientException, URISyntaxException {
        MicrodataHtmlClient client = new MicrodataHtmlClient(uri.toString());
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
        MicrodataHtmlClient client = new MicrodataHtmlClient(uri.toString());
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
