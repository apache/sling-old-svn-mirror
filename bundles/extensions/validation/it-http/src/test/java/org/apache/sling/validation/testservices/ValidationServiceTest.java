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
package org.apache.sling.validation.testservices;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.sling.SlingTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * These tests leverage the {@link ValidationPostOperation} to validate the given request parameters.
 * The according validation model enforces the properties "field1" matching regex=^\\\p{Upper}+$ and "field2" (having an arbitrary value).
 *
 */
public class ValidationServiceTest extends SlingTestBase {

    @Test
    public void testValidRequestModel1() throws IOException, JSONException {
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("sling:resourceType", new StringBody("validation/test/resourceType1"));
        entity.addPart("field1", new StringBody("HELLOWORLD"));
        entity.addPart("field2", new StringBody("30.01.1988"));
        entity.addPart(SlingPostConstants.RP_OPERATION, new StringBody("validation"));
        RequestExecutor re = getRequestExecutor().execute(getRequestBuilder().buildPostRequest
                ("/validation/testing/fakeFolder1/resource").withEntity(entity)).assertStatus(200);
        JSONObject jsonResponse = new JSONObject(re.getContent());
        assertTrue(jsonResponse.getBoolean("valid"));
    }

    @Test
    public void testInvalidRequestModel1() throws IOException, JSONException {
        MultipartEntity entity = new MultipartEntity();
        entity.addPart("sling:resourceType", new StringBody("validation/test/resourceType1"));
        entity.addPart("field1", new StringBody("Hello World"));
        entity.addPart(SlingPostConstants.RP_OPERATION, new StringBody("validation"));
        RequestExecutor re = getRequestExecutor().execute(getRequestBuilder().buildPostRequest
                ("/validation/testing/fakeFolder1/resource").withEntity(entity)).assertStatus(200);
        String content = re.getContent();
        JSONObject jsonResponse = new JSONObject(content);
        assertFalse(jsonResponse.getBoolean("valid"));
        JSONObject failure = jsonResponse.getJSONArray("failures").getJSONObject(0);
        assertEquals("Property does not match the pattern \"^\\p{Upper}+$\".", failure.get("message"));
        assertEquals("field1", failure.get("location"));
        assertEquals(10, failure.get("severity"));
        failure = jsonResponse.getJSONArray("failures").getJSONObject(1);
        assertEquals("Missing required property with name \"field2\".", failure.get("message"));
        assertEquals("", failure.get("location")); // location is empty as the property is not found (property name is part of the message rather)
        assertEquals(0, failure.get("severity"));
    }
}
