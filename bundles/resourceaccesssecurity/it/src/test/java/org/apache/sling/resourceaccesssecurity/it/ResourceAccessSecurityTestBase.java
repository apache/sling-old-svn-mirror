/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.resourceaccesssecurity.it;


import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.tools.sling.SlingTestBase;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResourceAccessSecurityTestBase extends SlingTestBase {

    public static final String TEST_USERNAME = "testUser";
    public static final String TEST_PASSWORD = "password";


    protected String getTestUsername() {
        return TEST_USERNAME;
    }

    protected String getTestPassword() {
        return TEST_PASSWORD;
    }


    protected void testRead(String username, String password,
                                String path, int expectedStatus,
                                String... expectedContent) throws Exception {
        if ( username == null )
        {
            // call without credentials
            getRequestExecutor().execute(
                    getRequestBuilder().buildGetRequest(path)
            ).assertStatus(expectedStatus).assertContentContains(expectedContent);
        }
        else
        {
            // call with credentials
            getRequestExecutor().execute(
                    getRequestBuilder().buildGetRequest(path)
                           .withCredentials(username, password)
            ).assertStatus(expectedStatus).assertContentContains(expectedContent);
            
        }
    }

    protected String testUpdate(String username, String password,
                              String path, int expectedStatus,
                              String... expectedContent) throws Exception {
        String addedValue = "addedValue" + UUID.randomUUID().toString();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("addedProperty", addedValue));

        if ( username == null )
        {
            // call without credentials
            getRequestExecutor().execute(
                    getRequestBuilder().buildPostRequest(path)
                            .withEntity(new UrlEncodedFormEntity(params))
            ).assertStatus(expectedStatus).assertContentContains(expectedContent);
        }
        else
        {
            // call with credentials
            getRequestExecutor().execute(
                    getRequestBuilder().buildPostRequest(path)
                            .withEntity(new UrlEncodedFormEntity(params))
            ).assertStatus(expectedStatus).assertContentContains(expectedContent);
           
        }

        return addedValue;
    }

}
