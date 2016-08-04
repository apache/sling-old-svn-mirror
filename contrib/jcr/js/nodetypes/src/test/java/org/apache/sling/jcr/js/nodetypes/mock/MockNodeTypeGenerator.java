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
package org.apache.sling.jcr.js.nodetypes.mock;

import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.NODETYPE_REQ_PRIMARY_TYPE_NAME1;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.NODETYPE_REQ_PRIMARY_TYPE_NAME2;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.OnParentVersionAction;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.sling.jcr.js.nodetypes.NodeTypesJSONServlet;
import org.apache.sling.jcr.js.nodetypes.test.JSONAssert;

/**
 * Generates NodeType mocks that will be returned when the servlet calles
 * NodeTypeManager.getAllNodeTypes(). It also mocks request, response, output
 * stream and so on to simulate the environment the servlet needs at runtime.
 * 
 */
public class MockNodeTypeGenerator {

	// mock classes
	protected SlingHttpServletRequest request = null;
	protected SlingHttpServletResponse response = null;
	protected Resource resource = null;
	protected Node currentNode = null;
	protected Session session = null;
	protected Workspace workspace = null;
	protected NodeTypeManager ntManager = null;
	protected NodeTypeIterator nodeTypeIterator = null;
	protected ByteArrayOutputStream outStream = null;
	private MockPropertyDefGenerator propertyGenerator = new MockPropertyDefGenerator();

	public void setUp() throws RepositoryException, IOException {
		// create mocks
		request = mock(SlingHttpServletRequest.class);
		response = mock(SlingHttpServletResponse.class);
		resource = mock(Resource.class);
		currentNode = mock(Node.class);
		session = mock(Session.class);
		workspace = mock(Workspace.class);
		ntManager = mock(NodeTypeManager.class);
		nodeTypeIterator = mock(NodeTypeIterator.class);
		outStream = new ByteArrayOutputStream();

		// stubbing
		when(request.getResource()).thenReturn(resource);
		when(request.getMethod()).thenReturn(HttpConstants.METHOD_GET);
		when(response.getWriter()).thenReturn(new PrintWriter(outStream, true));
		when(resource.adaptTo(Node.class)).thenReturn(currentNode);
		when(currentNode.getSession()).thenReturn(session);
		when(session.getWorkspace()).thenReturn(workspace);
		when(workspace.getNodeTypeManager()).thenReturn(ntManager);
		when(ntManager.getAllNodeTypes()).thenReturn(nodeTypeIterator);

	}

	public MockPropertyDefGenerator getPropertyGenerator() {
		return this.propertyGenerator;
	}

	public NodeDefinition getSimpleChildNodeDef(String name) {
		NodeDefinition childNodeDef1 = mock(NodeDefinition.class);
		NodeType[] reqPrimaryTypes = { getSimpleNodeTypeWithName(NODETYPE_REQ_PRIMARY_TYPE_NAME1),
				getSimpleNodeTypeWithName(NODETYPE_REQ_PRIMARY_TYPE_NAME2) };
		when(childNodeDef1.getRequiredPrimaryTypes()).thenReturn(reqPrimaryTypes);
		when(childNodeDef1.getName()).thenReturn(name);
		when(childNodeDef1.getOnParentVersion()).thenReturn(OnParentVersionAction.COPY);
		return childNodeDef1;
	}

	public NodeDefinition getCompleteChildNodeDef(String name) {
		NodeDefinition childNodeDef1 = mock(NodeDefinition.class);
		NodeType requiredPrimaryType1 = getSimpleNodeTypeWithName(NODETYPE_REQ_PRIMARY_TYPE_NAME1);
		NodeType requiredPrimaryType2 = getSimpleNodeTypeWithName(NODETYPE_REQ_PRIMARY_TYPE_NAME2);
		NodeType[] reqPrimaryTypes = { requiredPrimaryType1, requiredPrimaryType2 };
		when(childNodeDef1.getRequiredPrimaryTypes()).thenReturn(reqPrimaryTypes);
		when(childNodeDef1.getName()).thenReturn(name);
		when(childNodeDef1.getOnParentVersion()).thenReturn(OnParentVersionAction.VERSION);
		when(childNodeDef1.getDefaultPrimaryType()).thenReturn(requiredPrimaryType1);
		when(childNodeDef1.allowsSameNameSiblings()).thenReturn(Boolean.TRUE);
		when(childNodeDef1.isAutoCreated()).thenReturn(Boolean.TRUE);
		when(childNodeDef1.isMandatory()).thenReturn(Boolean.TRUE);
		when(childNodeDef1.isProtected()).thenReturn(Boolean.TRUE);
		return childNodeDef1;
	}

	public void assertEqualsWithServletResult(String filename) throws JSONException, ServletException,
			IOException {
		NodeTypesJSONServlet generationServlet = new NodeTypesJSONServlet();
		generationServlet.service(request, response);
		verify(response, never()).sendError(anyInt());
		String resultingJSON = new String(outStream.toByteArray(), "utf-8");


		String expectedNTJSON = getExpectedNTJSON(filename);
		
		JSONObject actualJsonObjectFromServlet = new JSONObject(new JSONTokener(resultingJSON));

		JSONAssert.assertEquals("Actual JSON: " + resultingJSON + "\nExpected JSON: " + expectedNTJSON, new JSONObject(
				new JSONTokener(expectedNTJSON)), actualJsonObjectFromServlet);
	}

	protected String getExpectedNTJSON(String filename) throws IOException {
		URL fileUri = MockNodeTypeGenerator.class.getResource("/expectedNTJSON/"+filename+".json");
		BufferedReader in = new BufferedReader(new FileReader(fileUri.getFile()));
		String currentLine = null;
		StringBuilder fileContent = new StringBuilder();
		while ((currentLine = in.readLine()) != null) {
			fileContent.append(currentLine);
		}
		return fileContent.toString();
	}

	public NodeType getSimpleNodeTypeWithName(String nodeTypeName) {
		NodeType nt1 = mock(NodeType.class);
		NodeType supertype = mock(NodeType.class);
		when(supertype.getName()).thenReturn("nt:base");
		NodeType[] supertypes = { supertype };
		when(nt1.getDeclaredSupertypes()).thenReturn(supertypes);
		when(nt1.getName()).thenReturn(nodeTypeName);
		return nt1;
	}
}
