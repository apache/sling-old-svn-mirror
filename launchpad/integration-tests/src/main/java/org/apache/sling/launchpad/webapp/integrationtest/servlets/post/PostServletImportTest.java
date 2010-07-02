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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
    			assertEquals(object, object2);
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
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
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);
        
        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);
        
        //make sure the name is what was inside the file.
        assertTrue(importedNodeUrl.endsWith("/nodeName"));
        
        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
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
        String jsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.json"));
        props.put(SlingPostConstants.RP_CONTENT, jsonContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
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
        
        String jsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport2.json"));
        props.put(SlingPostConstants.RP_CONTENT, jsonContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);

        //make sure the name is what was inside the file.
        assertTrue(importedNodeUrl.endsWith("/nodeName"));

        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);
        
        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);
        
        //make sure the name is what was inside the file.
        assertTrue(importedNodeUrl.endsWith("/nodeName"));
        
        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
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
        String xmlContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport.xml"));
        props.put(SlingPostConstants.RP_CONTENT, xmlContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "xml");
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);
        
        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
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
        
        String xmlContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimport2.xml"));
        props.put(SlingPostConstants.RP_CONTENT, xmlContent);
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "xml");
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, props);
        
        //make sure the name is what was inside the file.
        assertTrue(importedNodeUrl.endsWith("/nodeName"));
        
        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);
        
        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimportzip.json"));
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);
        
        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/testimportzip.json"));
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
        props.put(SlingPostConstants.RP_REDIRECT_TO, testPath + "/*");
        String importedNodeUrl = testClient.createNode(HTTP_BASE_URL + testPath, new NameValuePairList(props), null, true,
        		testFile, SlingPostConstants.RP_CONTENT_FILE, null);
        
        // assert content at new location
        String content = getContent(importedNodeUrl + ".3.json", CONTENT_TYPE_JSON);

		JSONObject jsonObj = new JSONObject(content);
		assertNotNull(jsonObj);

		//assert the imported content is there.
        String expectedJsonContent = (String)getStreamAsString(getClass().getResourceAsStream("/integration-test/servlets/post/importresults.json"));
		assertExpectedJSON(new JSONObject(expectedJsonContent), jsonObj);
    }
    
}
