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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.commons.testing.integration.NameValuePairList;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test content import via the MicrojaxPostServlet */
public class PostServletImportTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/sling-import-tests";
    static Random random = new Random();

    File testFile;

    /* (non-Javadoc)
	 * @see org.apache.sling.commons.testing.integration.HttpTestBase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		if (testFile != null) {
			//cleanup temp file
			testFile.delete();
		}
		super.tearDown();
	}

	static String getStreamAsString(InputStream is) throws IOException {
        final StringBuilder content = new StringBuilder();
        final byte [] buffer = new byte[16384];
        int n = 0;
        while( (n = is.read(buffer, 0, buffer.length)) > 0) {
            content.append(new String(buffer, 0, n));
        }
        return content.toString();
    }

	static String getStreamAsString(InputStream is, String charset) throws IOException {
		InputStreamReader reader = new InputStreamReader(is, charset);
        final StringBuilder content = new StringBuilder();
        final char [] buffer = new char[16384];
        int n = 0;
        while( (n = reader.read(buffer)) > 0) {
            content.append(buffer, 0, n);
        }
        return content.toString();
    }

    private File getTestFile(InputStream inputStream) throws IOException {
    	File tempFile = File.createTempFile("file-to-upload", null, new File("target"));
    	FileOutputStream outputStream = new FileOutputStream(tempFile);
    	byte[] bbuf = new byte[16384]; //16k
    	int len;
    	while ((len = inputStream.read(bbuf)) != -1) {
    		outputStream.write(bbuf, 0, len);
    	}
    	outputStream.flush();
    	outputStream.close();
    	return tempFile;
    }

    protected void assertExpectedJSON(JSONObject expectedJson, JSONObject actualJson) throws JSONException {
    	Iterator<String> keys = expectedJson.keys();
    	while (keys.hasNext()) {
    		String key = keys.next();

    		Object object = expectedJson.get(key);
    		Object object2 = actualJson.get(key);
			if (object instanceof JSONObject) {
				assertTrue(object instanceof JSONObject);
    			assertExpectedJSON((JSONObject)object, (JSONObject)object2);
			} else if (object instanceof JSONArray) {
				//compare the array
				assertTrue(object2 instanceof JSONArray);
				JSONArray actualArray = (JSONArray)object2;
				Set<Object> actualValuesSet = new HashSet<Object>();
				for (int i=0; i < actualArray.length(); i++) {
					actualValuesSet.add(actualArray.get(i));
				}

				JSONArray expectedArray = (JSONArray)object;
				for (int i=0; i < expectedArray.length(); i++) {
					assertTrue(actualValuesSet.contains(expectedArray.get(i)));
				}
    		} else {
    			assertEquals("Value of key: " + key, object, object2);
    		}
    	}
    }

    /**
     * Test import operation which replaces existing content
     */
    public void testImportReplace() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        //add node that will get replaced
        props.put("propTest", "propTestValue");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath + "/nodeName", props);

        //import with the replace option to replace the existing node.
        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport2.json"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        props.put(SlingPostConstants.RP_REPLACE, "true");
        String importedNodeUrl2 = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        //the new node should have the same path as the replaced node
        assertEquals(importedNodeUrl, importedNodeUrl2);

        // assert content at new location
        String content = getContent(importedNodeUrl2 + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);
		assertNull(jsonObj.optString("propTest", null)); //test property should be gone.

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }

    /**
     * SLING-1627: test import of content over existing content with the ':replaceProperties"
     * parameter set and the ":replace" property not set.
     */
    public void testImportReplaceProperties() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        //1. First import some initial content
        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        String jsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.json"));
        props.put(SlingPostConstants.RP_CONTENT, jsonContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);


		//2. Second, import on top of the node from #1 to replace some properties.

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String jsonContent2 = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport_replaceProps.json"));

        props.put(SlingPostConstants.RP_CONTENT, jsonContent2);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REPLACE, "false");
        props.put(SlingPostConstants.RP_REPLACE_PROPERTIES, "true");
        testClient.createNode(importedNodeUrl, props);

        // assert content at new location
        String content2 = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj2 = new JSONObject(content2);
		assertNotNull(jsonObj2);

		//assert the imported content is there.
        String expectedJsonContent2 = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport_replaceProps.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent2), jsonObj2);
    }

    /**
     * Test import operation which checks in versionable nodes.
     */
    public void testImportCheckinNodes() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport3.json"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        props.put(SlingPostConstants.RP_CHECKIN, "true");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert that the versionable node is checked in.
		assertFalse(jsonObj.getBoolean("jcr:isCheckedOut"));


		//now try with the checkin value set to false
		testFile.delete();
        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName2 = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName2);
        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport3.json"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        props.put(SlingPostConstants.RP_CHECKIN, "false");
        String importedNodeUrl2 = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        // assert content at new location
        String content2 = getContent(importedNodeUrl2 + ".json", CONTENT_TYPE_JSON);

		JSONObject jsonObj2 = new JSONObject(content2);
		assertNotNull(jsonObj2);

		//assert that the versionable node is checked in.
		assertTrue(jsonObj2.getBoolean("jcr:isCheckedOut"));
    }

    /**
     * SLING-2108 Test import operation which auto checks out versionable nodes.
     */
    public void testImportAutoCheckoutNodes() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        //1. first create some content to update.
        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport3.json"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        props.put(SlingPostConstants.RP_CHECKIN, "true");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert that the versionable node is checked in.
		assertFalse(jsonObj.getBoolean("jcr:isCheckedOut"));


		//2. try an update with the :autoCheckout value set to false
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
        						SlingPostConstants.OPERATION_IMPORT));
        postParams.add(new NameValuePair(SlingPostConstants.RP_CONTENT_TYPE, "json"));
        postParams.add(new NameValuePair(SlingPostConstants.RP_CHECKIN, "true"));
        postParams.add(new NameValuePair(SlingPostConstants.RP_REPLACE_PROPERTIES, "true"));
        postParams.add(new NameValuePair(SlingPostConstants.RP_AUTO_CHECKOUT, "false"));
        postParams.add(new NameValuePair(SlingPostConstants.RP_CONTENT, "{ \"abc\": \"def2\" }"));
        assertPostStatus(importedNodeUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, "Expected error from VersionException");

		//3. now try an update with the :autoCheckout value set to true
        postParams.clear();
        postParams.add(new NameValuePair(SlingPostConstants.RP_OPERATION,
				SlingPostConstants.OPERATION_IMPORT));
		postParams.add(new NameValuePair(SlingPostConstants.RP_CONTENT_TYPE, "json"));
		postParams.add(new NameValuePair(SlingPostConstants.RP_CHECKIN, "true"));
		postParams.add(new NameValuePair(SlingPostConstants.RP_REPLACE_PROPERTIES, "true"));
		postParams.add(new NameValuePair(SlingPostConstants.RP_AUTO_CHECKOUT, "true"));
		postParams.add(new NameValuePair(SlingPostConstants.RP_CONTENT, "{ \"abc\": \"def2\" }"));
		postParams.add(new NameValuePair(":http-equiv-accept", "application/json,*/*;q=0.9"));
        HttpMethod post = assertPostStatus(importedNodeUrl, HttpServletResponse.SC_CREATED, postParams, "Expected 201 status");
        
        String responseBodyAsString = post.getResponseBodyAsString();
		JSONObject responseJSON = new JSONObject(responseBodyAsString);
        JSONArray changes = responseJSON.getJSONArray("changes");
        JSONObject checkoutChange = changes.getJSONObject(0);
        assertEquals("checkout", checkoutChange.getString("type"));
		
        // assert content at new location
        String content2 = getContent(importedNodeUrl + ".json", CONTENT_TYPE_JSON);

		JSONObject jsonObj2 = new JSONObject(content2);
		assertNotNull(jsonObj2);
		
		//make sure it was really updated
		assertEquals("def2", jsonObj2.getString("abc"));
		
		//assert that the versionable node is checked back in.
		assertFalse(jsonObj.getBoolean("jcr:isCheckedOut"));		
    }
    
    /**
     * Test import operation for a posted json file
     */
    public void testImportJSONFromFile() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.json"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }

    /**
     * Test import operation for a posted json file without the optional name
     */
    public void testImportJSONFromFileWithoutOptionalName() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport2.json"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        //make sure the name is what was inside the file.
        assertTrue(importedNodeUrl.endsWith("/nodeName"));

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }

    /**
     * Test import operation for a posted json string
     */
    public void testImportJSONFromRequestParam() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        String jsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.json"));
        props.put(SlingPostConstants.RP_CONTENT, jsonContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }

    /**
     * Test import operation for a posted json string without the optional name
     */
    public void testImportJSONFromRequestParamWithoutOptionalName() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String jsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport2.json"));
        props.put(SlingPostConstants.RP_CONTENT, jsonContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);

        //make sure the name is what was inside the file.
        assertTrue(importedNodeUrl.endsWith("/nodeName"));

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }

    public void testImportXMLFromFile() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.xml"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "xml");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }

    public void testImportXMLFromFileWithoutOptionalName() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport2.xml"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "xml");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        //make sure the name is what was inside the file.
        assertTrue(importedNodeUrl.endsWith("/nodeName"));

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }

    public void testImportXMLFromRequestParam() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        String xmlContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.xml"));
        props.put(SlingPostConstants.RP_CONTENT, xmlContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "xml");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }

    public void testImportXMLFromRequestParamWithoutOptionalName() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String xmlContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport2.xml"));
        props.put(SlingPostConstants.RP_CONTENT, xmlContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "xml");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);

        //make sure the name is what was inside the file.
        assertTrue(importedNodeUrl.endsWith("/nodeName"));

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }


    public void testImportZipFromFile() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.zip"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "zip");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimportzip.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }

    public void testImportJarFromFile() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.jar"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "jar");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimportzip.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }


    public void testImportJCRXMLFromFile() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testnode_1287021810";
        props.put(SlingPostConstants.RP_NODE_NAME, testNodeName);
        testFile = getTestFile(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.jcr.xml"));
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "jcr.xml");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }




    protected String importNodeWithExactName(String testNodeName) throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        props.put(SlingPostConstants.RP_NODE_NAME, testNodeName);
        String jsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.json"));
        props.put(SlingPostConstants.RP_CONTENT, jsonContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");
        String location = testClient.createNode(HTTP_BASE_URL + testPath, props);

        // assert content at new location
        String content = getContent(location + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);

    	assertHttpStatus(location + DEFAULT_EXT, HttpServletResponse.SC_OK,
                "POST must redirect to created resource (" + location + ")");
        assertTrue("Node (" + location + ") must have exact name",
                !location.endsWith("/*"));
        assertTrue("Node (" + location + ") must created be under POST URL (" + testNode + ")",
                location.contains(testNode + "/"));
        assertTrue("Node (" + location + ") must have exact name '" + testNodeName + "'",
        		location.endsWith("/" + testNodeName));

		return location;
    }

    /**
     * SLING-1091: test create node with an exact node name (no filtering)
     */
    public void testImportNodeWithExactName() throws IOException, JSONException {
    	importNodeWithExactName("exactNodeName");
    }

    /**
     * SLING-1091: test error reporting when attempting to create a node with an
     * invalid exact node name.
     */
    public void testImportNodeWithInvalidExactName() throws IOException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_IMPORT));
		postParams.add(new NameValuePair(SlingPostConstants.RP_NODE_NAME, "exactNodeName*"));
        String jsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.json"));
		postParams.add(new NameValuePair(SlingPostConstants.RP_CONTENT, jsonContent));
		postParams.add(new NameValuePair(SlingPostConstants.RP_CONTENT_TYPE, "json"));
		postParams.add(new NameValuePair(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*"));

        //expect a 500 status since the name is invalid
        String location = HTTP_BASE_URL + testPath;
		assertPostStatus(location, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }

    /**
     * SLING-1091: test error reporting when attempting to import a node with an
     * already used node name.
     */
    public void testImportNodeWithAlreadyUsedExactName() throws IOException, JSONException {
    	String testNodeName = "alreadyUsedExactNodeName";
    	String location = importNodeWithExactName(testNodeName);


        //try to create the same node again, since same name siblings are not allowed an error should be
        // thrown
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new NameValuePair(SlingPostConstants.RP_NODE_NAME, testNodeName));
		//expect a 500 status since the name is not unique
		String postUrl = location.substring(0, location.lastIndexOf('/'));
		assertPostStatus(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }

    /**
     * SLING-2143: test import where json is in a UTF-8 charset
     */
    public void testImportJSONWithUTF8Content() throws IOException, JSONException {
        final String testPath = TEST_BASE_PATH;
        Map<String, String> props = new HashMap<String, String>();
        String testNode = testClient.createNode(HTTP_BASE_URL + testPath, props);
        urlsToDelete.add(testNode);

        props.clear();
        props.put(SlingPostConstants.RP_OPERATION,
        		SlingPostConstants.OPERATION_IMPORT);

        String testNodeName = "testNode_" + String.valueOf(random.nextInt());
        props.put(SlingPostConstants.RP_NODE_NAME_HINT, testNodeName);
        final String jsonContent = getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport_utf8.json"), "UTF-8");
        props.put(SlingPostConstants.RP_CONTENT, jsonContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, SERVLET_CONTEXT + testPath + "/*");

        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
		assertExpectedJSON(new JSONObject(jsonContent), jsonObj);
    }

}
