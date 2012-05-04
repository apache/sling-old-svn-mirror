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

package org.apache.sling.scripting.console;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.junit.Test;

public class ConsoleTestClient {
    private DefaultHttpClient httpClient = new DefaultHttpClient();
    private RequestExecutor executor = new RequestExecutor(httpClient);

    @Test
    public void testResourceResolver() throws Exception {
        RequestBuilder rb = new RequestBuilder("http://localhost:9000");

        final MultipartEntity entity = new MultipartEntity();
        // Add Sling POST options
        entity.addPart("lang", new StringBody("esp"));
        entity.addPart("code", new InputStreamBody(getClass().getResourceAsStream("/test.js"), "test.js"));
        executor.execute(
                rb.buildPostRequest("/system/console/scriptconsole.json")
                 .withEntity(entity)
                 .withCredentials("admin","admin")
            ).assertStatus(200);
    }
}
