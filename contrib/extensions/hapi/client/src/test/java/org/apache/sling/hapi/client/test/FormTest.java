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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
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

public class FormTest {
    private static final String GET_URL = "/test1";
    private static final String POST_URL = "/testpost1";
    private static final String OK_RESPONSE = "TEST_OK";
    private static final String FAIL_RESPONSE = "TEST_FAIL";

    @ClassRule
    public static final HttpServerRule httpServer = new HttpServerRule() {

        @Override
        protected void registerHandlers() {
            serverBootstrap.registerHandler(GET_URL, new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext)
                        throws HttpException, IOException {
                    final String html = IOUtils.toString(ItemsTest.class.getResourceAsStream("/items_forms.html"), "UTF-8"); 
                    HttpEntity entity = new StringEntity(html, "UTF-8");
                    httpResponse.setEntity(entity);
                }
            }).registerHandler(POST_URL, new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext)
                        throws HttpException, IOException {
                    if (!httpRequest.getRequestLine().getMethod().equals("POST")) {
                        httpResponse.setEntity(new StringEntity(FAIL_RESPONSE));
                    } else {
                        httpResponse.setEntity(new StringEntity(OK_RESPONSE));
                    }
                    httpResponse.setStatusCode(302);
                    httpResponse.setHeader("Location", GET_URL);
                }
            });
        }
    };
    
    @Test
    public void testForm() throws ClientException, URISyntaxException {
        MicrodataHtmlClient client = new MicrodataHtmlClient(httpServer.getURI().toString());
        Document doc = client.enter(GET_URL);
        Items items = doc.items();
        Assert.assertThat(items.length(), equalTo(1));
        Items form = doc.form("test");
        Assert.assertThat(form.length(), equalTo(2));

        List<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new BasicNameValuePair("f1", "val1"));

        // url encode enctype
        Document doc2 = form.at(0).submit(data);
        Assert.assertThat(doc2.items().length(), equalTo(1));

        // the multipart enctype
        Document doc3 = form.at(1).submit(data);
        Assert.assertThat(doc3.items().length(), equalTo(1));
    }
}