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

import static org.junit.Assert.fail;

import java.io.File;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.http.RetryingContentChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP Client for the Felix webconsole - simplistic for now */
public class WebconsoleClient {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final RequestExecutor executor;
    private final RequestBuilder builder;
    private final String username;
    private final String password;
    
    public static final String JSON_KEY_ID = "id";
    public static final String JSON_KEY_VERSION = "version";
    public static final String JSON_KEY_DATA = "data";
    public static final String JSON_KEY_STATE = "state";
    public static final String CONSOLE_BUNDLES_PATH = "/system/console/bundles";
    
    public WebconsoleClient(String slingServerUrl, String username, String password) {
        this.builder = new RequestBuilder(slingServerUrl);
        this.executor = new RequestExecutor(new DefaultHttpClient());
        this.username = username;
        this.password = password;
    }
    
    public void uninstallBundle(String symbolicName, File f) throws Exception {
        final long bundleId = getBundleId(symbolicName);
        
        final MultipartEntity entity = new MultipartEntity();
        entity.addPart("action",new StringBody("uninstall"));
        executor.execute(
                builder.buildPostRequest(CONSOLE_BUNDLES_PATH+"/"+bundleId)
                .withCredentials(username, password)
                .withEntity(entity)
        ).assertStatus(200);
    }
    
    /** Install a bundle using the Felix webconsole HTTP interface, with a specific start level */
    public void installBundle(File f, boolean startBundle) throws Exception {
        installBundle(f, startBundle, 0);
    }
    
    /** Install a bundle using the Felix webconsole HTTP interface, with a specific start level */
    public void installBundle(File f, boolean startBundle, int startLevel) throws Exception {
        
        // Setup request for Felix Webconsole bundle install
        final MultipartEntity entity = new MultipartEntity();
        entity.addPart("action",new StringBody("install"));
        if(startBundle) {
            entity.addPart("bundlestart", new StringBody("true"));
        }
        entity.addPart("bundlefile", new FileBody(f));
        
        if(startLevel > 0) {
            entity.addPart("bundlestartlevel", new StringBody(String.valueOf(startLevel)));
            log.info("Installing bundle {} at start level {}", f.getName(), startLevel);
        } else {
            log.info("Installing bundle {} at default start level", f.getName());
        }
        
        // Console returns a 302 on success (and in a POST this
        // is not handled automatically as per HTTP spec)
        executor.execute(
                builder.buildPostRequest(CONSOLE_BUNDLES_PATH)
                .withCredentials(username, password)
                .withEntity(entity)
        ).assertStatus(302);
    }
    
    /** Check that specified bundle is installed - must be called
     *  before other methods that take a symbolicName parameter, 
     *  in case installBundle was just called and the actual 
     *  installation hasn't happened yet. */
    public void checkBundleInstalled(String symbolicName, int timeoutSeconds) {
        final String path = getBundlePath(symbolicName, ".json");
        new RetryingContentChecker(executor, builder).check(path, 200, timeoutSeconds, 500);
    }
    
    private JSONObject getBundleData(String symbolicName) throws Exception {
        // This returns a data structure like
        // {"status":"Bundle information: 173 bundles in total - all 173 bundles active.","s":[173,171,2,0,0],"data":
        //  [
        //      {"id":0,"name":"System Bundle","fragment":false,"stateRaw":32,"state":"Active","version":"3.0.7","symbolicName":"org.apache.felix.framework","category":""},
        //  ]}
        final String path = getBundlePath(symbolicName, ".json");
        final String content = executor.execute(
                builder.buildGetRequest(path)
                .withCredentials(username, password)
        ).assertStatus(200)
        .getContent();
        
        final JSONObject root = new JSONObject(content);
        if(!root.has(JSON_KEY_DATA)) {
            fail(path + " does not provide '" + JSON_KEY_DATA + "' element, JSON content=" + content);
        }
        final JSONArray data = root.getJSONArray(JSON_KEY_DATA);
        if(data.length() < 1) {
            fail(path + "." + JSON_KEY_DATA + " is empty, JSON content=" + content);
        }
        final JSONObject bundle = data.getJSONObject(0);
        if(!bundle.has(JSON_KEY_STATE)) {
            fail(path + ".data[0].state missing, JSON content=" + content);
        }
        return bundle;
    }

    /** Get bundle id */
    public long getBundleId(String symbolicName) throws Exception {
        final JSONObject bundle = getBundleData(symbolicName);
        return bundle.getLong(JSON_KEY_ID);
    }
    
    /** Get bundle version **/
    public String getBundleVersion(String symbolicName) throws Exception {
        final JSONObject bundle = getBundleData(symbolicName);
        return bundle.getString(JSON_KEY_VERSION);
    }
    
    /** Get specified bundle state */
    public String getBundleState(String symbolicName) throws Exception {
        final JSONObject bundle = getBundleData(symbolicName);
        return bundle.getString(JSON_KEY_STATE);
    }
    
    /** Start specified bundle */
    public void startBundle(String symbolicName) throws Exception {
        // To start the bundle we POST action=start to its URL
        final String path = getBundlePath(symbolicName, null);
        log.info("Starting bundle {} via {}", symbolicName, path);
        
        final MultipartEntity entity = new MultipartEntity();
        entity.addPart("action",new StringBody("start"));
        executor.execute(
                builder.buildPostRequest(path)
                .withCredentials(username, password)
                .withEntity(entity)
        ).assertStatus(200);
    }
    
    private String getBundlePath(String symbolicName, String extension) {
        return CONSOLE_BUNDLES_PATH + "/" + symbolicName 
        + (extension == null ? "" : extension);
    }
}
