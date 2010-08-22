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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.NameValuePairList;
import org.apache.sling.launchpad.webapp.integrationtest.AbstractAuthenticatedTest;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test node updates via the MicrojaxPostServlet */
public class PostServletUpdateTest extends AbstractAuthenticatedTest {
    public static final String TEST_BASE_PATH = "/sling-tests";
    private String postUrl;
	private String testUserId = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

    
   /* (non-Javadoc)
	 * @see org.apache.sling.commons.testing.integration.HttpTestBase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		if (testUserId != null) {
			//remove the test user if it exists.
			String postUrl = HTTP_BASE_URL + "/system/userManager/user/" + testUserId + ".delete.html";
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			assertAuthenticatedAdminPostStatus(postUrl, HttpServletResponse.SC_OK, postParams, null);
		}
		super.tearDown();
	}


public void testPostPathIsUnique() throws IOException {
        assertHttpStatus(postUrl, HttpServletResponse.SC_NOT_FOUND,
                "Path must not exist before test: " + postUrl);
    }

    public void testUpdateWithChanges() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");

        props.put("./a","789");
        // the testClient method is called createNode but all it does is a POST,
        // so it can be used for updates as well
        final String newLocation = testClient.createNode(location, props);
        assertEquals("Location must not changed after POST to existing node",location,newLocation);
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("789456", content, "out.println(data.a + data.b)");
    }

    public void testUpdateNoChanges() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");

        props.clear();
        // the testClient method is called createNode but all it does is a POST,
        // so it can be used for updates as well
        final String newLocation = testClient.createNode(location, props);
        assertEquals("Location must not changed after POST to existing node",location,newLocation);
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");
    }

    public void testUpdateSomeChanges() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./a","123");
        props.put("./b","456");
        props.put("C","not stored");

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123456", content, "out.println(data.a + data.b)");

        props.clear();
        props.put("./b","457");
        props.put("C","still not stored");

        // the testClient method is called createNode but all it does is a POST,
        // so it can be used for updates as well
        final String newLocation = testClient.createNode(location, props);
        assertEquals("Location must not changed after POST to existing node",location,newLocation);
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("123457", content, "out.println(data.a + data.b)");
    }

    public void testMultivalueHint() throws IOException {
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("./f","123");
        props.put("./f@TypeHint", "String[]");
        props.put("./g","456");

        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertTrue(content.indexOf("\"f\":[\"123\"]") > 0);
        assertTrue(content.indexOf("\"g\":\"456\"") > 0);
    }

    public void testMixinTypes() throws IOException, JSONException {
        
        // create a node without mixin node types
        final Map <String, String> props = new HashMap <String, String> ();
        props.put("jcr:primaryType","nt:unstructured");
        final String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);
        
        // assert no mixins
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        JSONObject json = new JSONObject(content);
        assertFalse("jcr:mixinTypes not expected to be set", json.has("jcr:mixinTypes"));
        
        // add mixin
        props.clear();
        props.put("jcr:mixinTypes", "mix:versionable");
        testClient.createNode(location, props);
        
        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        json = new JSONObject(content);
        assertTrue("jcr:mixinTypes expected after setting them", json.has("jcr:mixinTypes"));
        
        Object mixObject = json.get("jcr:mixinTypes");
        assertTrue("jcr:mixinTypes must be an array", mixObject instanceof JSONArray);
        
        JSONArray mix = (JSONArray) mixObject;
        assertTrue("jcr:mixinTypes must have a single entry", mix.length() == 1);
        assertEquals("jcr:mixinTypes must have correct value", "mix:versionable", mix.get(0));

        // remove mixin
        props.clear();
        props.put("jcr:mixinTypes@Delete", "-");
        testClient.createNode(location, props);

        content = getContent(location + ".json", CONTENT_TYPE_JSON);
        json = new JSONObject(content);
        assertTrue("no jcr:mixinTypes expected after clearing it", !json.has("jcr:mixinTypes"));
    }
    
    /**
     * Test for SLING-897 fix: 
     * 1. Updating a property requires jcr:modifyProperties privilege on node.
     * 2. When changing an existing property observers should receive a PROPERTY_CHANGED event instead 
     *     of a PROPERTY_REMOVED event and a PROPERTY_ADDED event
     */
    public void testUpdatePropertyPrivilegesAndEvents() throws IOException, JSONException, RepositoryException, InterruptedException {
    	//1. Create user as admin (OK)
        // curl -F:name=myuser -Fpwd=password -FpwdConfirm=password http://admin:admin@localhost:8080/system/userManager/user.create.html
    	testUserId = createTestUser();

    	//2. Create node as admin (OK)
        // curl -F:nameHint=node -FpropOne=propOneValue1 -FpropOne=propOneValue2 -FpropTwo=propTwoValue http://admin:admin@localhost:8080/test/
        final String createTestNodeUrl = postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        NameValuePairList clientNodeProperties = new NameValuePairList();
        clientNodeProperties.add(SlingPostConstants.RP_NODE_NAME_HINT, getName());
        clientNodeProperties.add("propOne", "propOneValue1");
        clientNodeProperties.add("propOne", "propOneValue2");
        clientNodeProperties.add("propTwo", "propTwoValue");
    	String testNodeUrl = testClient.createNode(createTestNodeUrl, clientNodeProperties, null, false);

        String content = getContent(testNodeUrl + ".json", CONTENT_TYPE_JSON);
        JSONObject json = new JSONObject(content);
        Object propOneObj = json.opt("propOne");
        assertTrue(propOneObj instanceof JSONArray);
        assertEquals(2, ((JSONArray)propOneObj).length());
        assertEquals("propOneValue1", ((JSONArray)propOneObj).get(0));
        assertEquals("propOneValue2", ((JSONArray)propOneObj).get(1));
    	
        Object propTwoObj = json.opt("propTwo");
        assertTrue(propTwoObj instanceof String);
        assertEquals("propTwoValue", propTwoObj);
    	
    	
        //3. Attempt to update property of node as testUser (500: javax.jcr.AccessDeniedException: /test/node/propOne: not allowed to add or modify item)
        // curl -FpropOne=propOneValueChanged -FpropTwo=propTwoValueChanged1 -FpropTwo=propTwoValueChanged2 http://myuser:password@localhost:8080/test/node
    	List<NameValuePair> postParams = new ArrayList<NameValuePair>();
    	postParams.add(new NameValuePair("propOne", "propOneValueChanged"));
    	postParams.add(new NameValuePair("propTwo", "propTwoValueChanged1"));
    	postParams.add(new NameValuePair("propTwo", "propTwoValueChanged2"));
		Credentials testUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");
		String expectedMessage = "Expected javax.jcr.AccessDeniedException";
    	assertAuthenticatedPostStatus(testUserCreds, testNodeUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, expectedMessage);
    	
        //4. Grant jcr:modifyProperties rights to testUser as admin (OK)
        // curl -FprincipalId=myuser -Fprivilege@jcr:modifyProperties=granted http://admin:admin@localhost:8080/test/node.modifyAce.html
        Map<String, String> nodeAceProperties = new HashMap<String, String>();
        nodeAceProperties.put("principalId", testUserId);
        nodeAceProperties.put("privilege@jcr:modifyProperties", "granted");
    	testClient.createNode(testNodeUrl + ".modifyAce.html", nodeAceProperties);
    	
        //use a davex session to verify the correct JCR events are delivered
        Repository repository = JcrUtils.getRepository(HTTP_BASE_URL + "/server/");
        Session jcrSession = null;
        TestEventListener listener = new TestEventListener();
        ObservationManager observationManager = null;
        try {
            jcrSession = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            observationManager = jcrSession.getWorkspace().getObservationManager();
        	String testNodePath = testNodeUrl.substring(HTTP_BASE_URL.length());
            observationManager.addEventListener(listener, 
					Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, //event types
					testNodePath, //absPath
					true, //isDeep 
					null, //uuid
					null, //nodeTypeName
					false); //noLocal
        
            //5. Attempt to update properties of node (OK)
            // curl -FpropOne=propOneValueChanged -FpropTwo=propTwoValueChanged1 -FpropTwo=propTwoValueChanged2 http://myuser:password@localhost:8080/test/node
        	assertAuthenticatedPostStatus(testUserCreds, testNodeUrl, HttpServletResponse.SC_OK, postParams, expectedMessage);
        	
        	//verify the change happened
            String afterUpdateContent = getContent(testNodeUrl + ".json", CONTENT_TYPE_JSON);
            JSONObject afterUpdateJson = new JSONObject(afterUpdateContent);
            Object afterUpdatePropOneObj = afterUpdateJson.opt("propOne");
            assertTrue(afterUpdatePropOneObj instanceof JSONArray);
            assertEquals(1, ((JSONArray)afterUpdatePropOneObj).length());
            assertEquals("propOneValueChanged", ((JSONArray)afterUpdatePropOneObj).get(0));
        	
            Object afterUpdatePropTwoObj = afterUpdateJson.opt("propTwo");
            assertTrue(afterUpdatePropTwoObj instanceof JSONArray);
            assertEquals(2, ((JSONArray)afterUpdatePropTwoObj).length());
            assertEquals("propTwoValueChanged1", ((JSONArray)afterUpdatePropTwoObj).get(0));
            assertEquals("propTwoValueChanged2", ((JSONArray)afterUpdatePropTwoObj).get(1));
            
        	//wait for the expected JCR events to be delivered
			for (int second = 0; second < 15; second++) {
				if (listener.getEventBundlesProcessed() > 0) {
					break;
				}
				Thread.sleep(1000);
			}
			
			assertEquals("One property added event was expected: " + listener.toString(), 
					1, listener.addedProperties.size());
			assertEquals(testNodePath + "/propTwo", listener.addedProperties.get(0));
			assertEquals("One property removed event was expected: " + listener.toString(), 
					1, listener.removedProperties.size());
			assertEquals(testNodePath + "/propTwo", listener.removedProperties.get(0));
			assertEquals("One property changed event was expected: " + listener.toString(), 
					1, listener.changedProperties.size());
			assertEquals(testNodePath + "/propOne", listener.changedProperties.get(0));
        } finally {
        	//cleanup
        	if (observationManager != null) {
            	observationManager.removeEventListener(listener);
        	}
            jcrSession.logout();
            repository = null;
        }
    }
    
	protected class TestEventListener implements EventListener {
		protected List<String> changedProperties = new ArrayList<String>(); 
		protected List<String> addedProperties = new ArrayList<String>(); 
		protected List<String> removedProperties = new ArrayList<String>(); 
		
		protected int eventBundlesProcessed = 0;
		
		public void onEvent(EventIterator eventIterator) {
			try {
				while (eventIterator.hasNext()) {
					Event event = eventIterator.nextEvent();
					int type = event.getType();
					switch (type) {
						case Event.PROPERTY_CHANGED:
							changedProperties.add(event.getPath());
							break;
						case Event.PROPERTY_ADDED:
							addedProperties.add(event.getPath());
							break;
						case Event.PROPERTY_REMOVED:
							removedProperties.add(event.getPath());
							break;
					}
				}
				eventBundlesProcessed++;
			} catch (RepositoryException e) {
				fail(e.getMessage());
			}
		}
		
		public int getEventBundlesProcessed() {
			return eventBundlesProcessed;
		}

		public void clear() {
			changedProperties.clear();
			addedProperties.clear();
			removedProperties.clear();
			eventBundlesProcessed = 0;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "TestEventListener [addedProperties=" + Arrays.toString(addedProperties.toArray())
					+ ", changedProperties=" + Arrays.toString(changedProperties.toArray())
					+ ", removedProperties=" + Arrays.toString(removedProperties.toArray()) + "]";
		}
	}
    
}