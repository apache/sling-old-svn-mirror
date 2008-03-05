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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;


/** Test the {link JsonQueryServlet) functionality. 
 *  We don't need to test the repository query feature, just
 *  make sure that the query servlet parameters are interpreted correctly.
 */
public class JsonQueryServletTest extends HttpTestBase {

    private String testFolderUrl;
    private final String testPath = "/" + getClass().getSimpleName() + "_" + System.currentTimeMillis();
    private final static String counterCode = "out.print(data.length);";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        testFolderUrl = testClient.createNode(HTTP_BASE_URL + testPath, null);
        
        // create subfolders A..E with subnodes 0..4, each contains its name as a text property
        for(char folder = 'A'; folder <= 'E'; folder++) {
            final Map<String, String> props = new HashMap<String, String> ();
            props.put("creator", getClass().getSimpleName());
            props.put("text", "folder " + folder);
            final String subfolderUrl = testClient.createNode(testFolderUrl + "/folder" + folder, props);
            for(int i=0; i < 5; i++) {
                props.put("text", "folder " + folder + " node "+ i);
                testClient.createNode(subfolderUrl + "/node" + i, props);
            }
            
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
        if(testFolderUrl != null) {
            testClient.delete(WEBDAV_BASE_URL + testPath);
        }
    }
    
    private void assertCount(int expectedCount, String statement, String queryType, int offset, int rows) 
    throws IOException {
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("statement", statement));
        if(queryType != null) {
            params.add(new NameValuePair("queryType", queryType));
        }
        if(offset > 0) {
            params.add(new NameValuePair("offset", String.valueOf(offset)));
        }
        if(rows > 0) {
            params.add(new NameValuePair("rows", String.valueOf(rows)));
        }
        final String json = getContent(testFolderUrl + ".query.json", CONTENT_TYPE_JSON, params);
        assertJavascript(
                expectedCount + ".0", 
                json, 
                counterCode,
                "statement=" + statement + ", queryType=" + queryType
        );
    }
    
    public void testFolderQuery() throws IOException {
        assertCount(1, "/" + testPath + "/folderC", "xpath", 0, 0);
    }
    
    public void testSubFolderQuery() throws IOException {
        assertCount(5, "/" + testPath + "/folderA/*", "xpath", 0, 0);
    }
    
    public void testDefaultQueryType() throws IOException {
        assertCount(5, "/" + testPath + "/folderE/*", null, 0, 0);
    }
    
    public void testSql() throws IOException {
        final String query = "select * from nt:unstructured where jcr:path like '" + testPath + "/folderB/%'";
        assertCount(5, query, "sql", 0, 0);
    }
    
    public void testOffset() throws IOException {
        assertCount(3, "/" + testPath + "/folderC/*", "xpath", 2, 0);
    }
    
    public void testRows() throws IOException {
        assertCount(2, "/" + testPath + "/folderC/*", "xpath", 0, 2);
    }
    
    public void testPropertyParam() throws IOException {
        final String url = testFolderUrl + ".query.json";
        final String statement = "/" + testPath + "/folderB/node3";
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("statement", statement));
        
        String json = getContent(url, CONTENT_TYPE_JSON, params);
        assertJavascript("1.0", json, counterCode);
        assertJavascript("ok", json, "if(!data[0].text) out.print('ok')");
        assertJavascript("ok", json, "if(!data[0].creator) out.print('ok')");
        
        params.add(new NameValuePair("property", "text"));
        json = getContent(url, CONTENT_TYPE_JSON, params);
        assertJavascript("1.0", json, counterCode);
        assertJavascript("ok", json, "if(data[0].text) out.print('ok')");
        assertJavascript("folder B node 3", json, "out.print(data[0].text)");
        assertJavascript("ok", json, "if(!data[0].creator) out.print('ok')");
        
        params.add(new NameValuePair("property", "creator"));
        json = getContent(url, CONTENT_TYPE_JSON, params);
        assertJavascript("1.0", json, counterCode);
        assertJavascript("ok", json, "if(data[0].text) out.print('ok')");
        assertJavascript("folder B node 3", json, "out.print(data[0].text)");
        assertJavascript("ok", json, "if(data[0].creator) out.print('ok')");
        assertJavascript(getClass().getSimpleName(), json, "out.print(data[0].creator)");
    }
}
