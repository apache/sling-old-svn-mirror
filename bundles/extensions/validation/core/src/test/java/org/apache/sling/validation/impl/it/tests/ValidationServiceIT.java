/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl.it.tests;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * These tests leverage the {@link ValidationPostOperation} to validate the given request parameters.
 * The according validation model enforces the properties "field1" matching regex=^\\\p{Upper}+$ and "field2" (having an arbitrary value).
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ValidationServiceIT extends ValidationTestSupport {

    protected DefaultHttpClient defaultHttpClient;

    protected RequestExecutor requestExecutor;

    @Before
    public void setup() throws IOException {
        defaultHttpClient = new DefaultHttpClient();
        requestExecutor = new RequestExecutor(defaultHttpClient);
    }

    @Test
    public void testValidRequestModel1() throws IOException, JsonException {
        final String url = String.format("http://localhost:%s", httpPort());
        final RequestBuilder requestBuilder = new RequestBuilder(url);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("sling:resourceType", new StringBody("validation/test/resourceType1"));
        entity.addPart("field1", new StringBody("HELLOWORLD"));
        entity.addPart("field2", new StringBody("30.01.1988"));
        entity.addPart(SlingPostConstants.RP_OPERATION, new StringBody("validation"));
        RequestExecutor re = requestExecutor.execute(requestBuilder.buildPostRequest
                ("/validation/testing/fakeFolder1/resource").withEntity(entity)).assertStatus(200);
        String content = re.getContent();
        JsonObject jsonResponse = Json.createReader(new StringReader(content)).readObject();
        assertTrue(jsonResponse.getBoolean("valid"));
    }

    @Test
    public void testInvalidRequestModel1() throws IOException, JsonException {
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("sling:resourceType", new StringBody("validation/test/resourceType1"));
        entity.addPart("field1", new StringBody("Hello World"));
        entity.addPart(SlingPostConstants.RP_OPERATION, new StringBody("validation"));
        final String url = String.format("http://localhost:%s", httpPort());
        RequestBuilder requestBuilder = new RequestBuilder(url);
        RequestExecutor re = requestExecutor.execute(requestBuilder.buildPostRequest
                ("/validation/testing/fakeFolder1/resource").withEntity(entity)).assertStatus(200);
        String content = re.getContent();
        JsonObject jsonResponse = Json.createReader(new StringReader(content)).readObject();
        assertFalse(jsonResponse.getBoolean("valid"));
        JsonObject failure = jsonResponse.getJsonArray("failures").getJsonObject(0);
        assertEquals("Property does not match the pattern \"^\\p{Upper}+$\".", failure.getString("message"));
        assertEquals("field1", failure.getString("location"));
        assertEquals(10, failure.getInt("severity"));
        failure = jsonResponse.getJsonArray("failures").getJsonObject(1);
        assertEquals("Missing required property with name \"field2\".", failure.getString("message"));
        assertEquals("", failure.getString("location")); // location is empty as the property is not found (property name is part of the message rather)
        assertEquals(0, failure.getInt("severity"));
    }
    
    @Test
    public void testPostProcessorWithInvalidModel() throws IOException, JsonException {
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("sling:resourceType", new StringBody("validation/test/resourceType1"));
        entity.addPart("field1", new StringBody("Hello World"));
        final String url = String.format("http://localhost:%s", httpPort());
        RequestBuilder requestBuilder = new RequestBuilder(url);
        // test JSON response, because the HTML response overwrites the original exception (https://issues.apache.org/jira/browse/SLING-6703)
        RequestExecutor re = requestExecutor.execute(requestBuilder.buildPostRequest
                ("/content/validated/invalidresource").withEntity(entity).withHeader("Accept", "application/json").withCredentials("admin", "admin")).assertStatus(500);
        String content = re.getContent();
        JsonObject jsonResponse = Json.createReader(new StringReader(content)).readObject();
        
        JsonObject error = jsonResponse.getJsonObject("error");
        assertEquals("org.apache.sling.validation.impl.postprocessor.InvalidResourcePostProcessorException", error.getString("class"));
        assertEquals("Validation errors: field1 : Property does not match the pattern \"^\\p{Upper}+$\"., Missing required property with name \"field2\".", error.getString("message"));
    }
}
