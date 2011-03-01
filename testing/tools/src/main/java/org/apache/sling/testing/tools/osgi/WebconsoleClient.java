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
package org.apache.sling.testing.tools.osgi;

import java.io.File;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP Client for the Felix webconsole - simplistic for now */
public class WebconsoleClient {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final RequestExecutor executor;
    private final RequestBuilder builder;
    private final String username;
    private final String password;
    
    public WebconsoleClient(String slingServerUrl, String username, String password) {
        this.builder = new RequestBuilder(slingServerUrl);
        this.executor = new RequestExecutor(new DefaultHttpClient());
        this.username = username;
        this.password = password;
    }
    
    /** Install a bundle using the Felix webconsole HTTP interface */
    public void installBundle(File f, boolean startBundle) throws Exception {
        log.info("Installing additional bundle {}", f.getName());
        
        // Setup request for Felix Webconsole bundle install
        final MultipartEntity entity = new MultipartEntity();
        entity.addPart("action",new StringBody("install"));
        if(startBundle) {
            entity.addPart("bundlestart", new StringBody("true"));
        }
        entity.addPart("bundlefile", new FileBody(f));
        
        // Console returns a 302 on success (and in a POST this
        // is not handled automatically as per HTTP spec)
        executor.execute(
                builder.buildPostRequest("/system/console/bundles")
                .withCredentials(username, password)
                .withEntity(entity)
        ).assertStatus(302);
    }
}
