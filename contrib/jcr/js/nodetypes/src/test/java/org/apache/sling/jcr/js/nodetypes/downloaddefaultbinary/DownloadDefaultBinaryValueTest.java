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
package org.apache.sling.jcr.js.nodetypes.downloaddefaultbinary;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.jcr.js.nodetypes.GenerationConstants;
import org.apache.sling.jcr.js.nodetypes.downloaddefaultbinary.DownloadDefaultBinaryValueServlet;
import org.apache.sling.jcr.js.nodetypes.mock.MockPropertyDefGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the DownloadDefaultBinaryValueServlet
 *
 */
public class DownloadDefaultBinaryValueTest {

	// mock classes
	private SlingHttpServletRequest request = null;
	private SlingHttpServletResponse response = null;
	private Resource resource = null;
	private Node rootNode = null;
	private Session session = null;
	private Workspace workspace = null;
	private NodeTypeManager ntManager = null;
	private ByteArrayOutputStream outStream = null;
	private ResourceResolver resourceResolver = null;
	private MockPropertyDefGenerator propDefGenerator = null;

	@Before
	public void setUp() throws RepositoryException, IOException{
		// create mocks
		request = mock(SlingHttpServletRequest.class);
		response = mock(SlingHttpServletResponse.class);
		resourceResolver = mock(ResourceResolver.class);
		resource = mock(Resource.class);
		rootNode = mock(Node.class);
		session = mock(Session.class);
		workspace = mock(Workspace.class);
		ntManager = mock(NodeTypeManager.class);
		outStream = new ByteArrayOutputStream();
		
		// stubbing
		when(request.getMethod()).thenReturn(HttpConstants.METHOD_GET);
		when(request.getResourceResolver()).thenReturn(resourceResolver);
		when(resourceResolver.getResource("/")).thenReturn(resource);
		when(resource.adaptTo(Node.class)).thenReturn(rootNode);
		when(rootNode.getSession()).thenReturn(session);
		when(session.getWorkspace()).thenReturn(workspace);
		when(workspace.getNodeTypeManager()).thenReturn(ntManager);
		when(response.getWriter()).thenReturn(new PrintWriter(outStream, true));
		propDefGenerator = new MockPropertyDefGenerator();
	}

	private void invokeServletWithDifferentPropertyDefs() throws NoSuchNodeTypeException, RepositoryException,
			ValueFormatException, ServletException, IOException {
		NodeType nodeType = mock(NodeType.class);
		when(ntManager.getNodeType("ns:ntName")).thenReturn(nodeType);
		PropertyDefinition[] propDefs = propDefGenerator.getDifferentPropertyDefinitions();
		when(nodeType.getPropertyDefinitions()).thenReturn(propDefs);
		DownloadDefaultBinaryValueServlet downloadServlet = new DownloadDefaultBinaryValueServlet();
		downloadServlet.service(request, response);
	}

	private void invokeServletWithEqualPropertyDefs() throws NoSuchNodeTypeException, RepositoryException,
			ValueFormatException, ServletException, IOException {
		NodeType nodeType = mock(NodeType.class);
		when(ntManager.getNodeType("ns:ntName")).thenReturn(nodeType);
		PropertyDefinition[] propDefs = propDefGenerator.getEqualPropertyDefinitions();
		when(nodeType.getPropertyDefinitions()).thenReturn(propDefs);
		DownloadDefaultBinaryValueServlet downloadServlet = new DownloadDefaultBinaryValueServlet();
		downloadServlet.service(request, response);
	}
	
	@Test
	public void testSuccessfulSingleMatchWithIndex() throws ServletException, IOException, NoSuchNodeTypeException, RepositoryException {
		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef/binary/true/true/true/true/version/1.default_binary_value.bin");
		invokeServletWithDifferentPropertyDefs();
		verify(response, never()).sendError(anyInt());
		Assert.assertEquals(GenerationConstants.DEFAULT_VALUE_BINARY_1, new String(outStream.toByteArray()));
	}
	
	@Test
	public void testSuccessfulSingleMatchWithoutIndex() throws ServletException, IOException, NoSuchNodeTypeException, RepositoryException {
		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef/binary/true/true/true/true/version/default_binary_value.bin");
		invokeServletWithDifferentPropertyDefs();
		verify(response, never()).sendError(anyInt());
		Assert.assertEquals(GenerationConstants.DEFAULT_VALUE_BINARY_0, new String(outStream.toByteArray()));
	}
	
	@Test
	public void testSuccessfulShortenedSingleMatchWithoutIndex() throws ServletException, IOException, NoSuchNodeTypeException, RepositoryException {
		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef/binary/true/true/true/false/default_binary_value.bin");
		testDifferentPropertyDefsWithExpectedBinary0();

		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef/binary/true/true/false/default_binary_value.bin");
		testDifferentPropertyDefsWithExpectedBinary0();

		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef/binary/true/false/default_binary_value.bin");
		testDifferentPropertyDefsWithExpectedBinary0();
		
		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef/boolean/default_binary_value.bin");
		testDifferentPropertyDefsWithExpectedBinary0();

		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef2/default_binary_value.bin");
		testDifferentPropertyDefsWithExpectedBinary0();
	}

	private void testDifferentPropertyDefsWithExpectedBinary0() throws NoSuchNodeTypeException, RepositoryException,
			ValueFormatException, ServletException, IOException {
		invokeServletWithDifferentPropertyDefs();
		verify(response, never()).sendError(anyInt());
		Assert.assertEquals(GenerationConstants.DEFAULT_VALUE_BINARY_0, new String(outStream.toByteArray()));
	}
	
	@Test
	public void testSuccessfulSingleMatchWithInvalidIndex() throws ServletException, IOException, NoSuchNodeTypeException, RepositoryException {
		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef/binary/true/true/true/true/version/5.default_binary_value.bin");
		invokeServletWithDifferentPropertyDefs();
		verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	
	@Test
	public void testUnsuccessfulMultipleMatches() throws ServletException, IOException, NoSuchNodeTypeException, RepositoryException {
		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef/binary/true/true/true/true/version/5.default_binary_value.bin");
		invokeServletWithEqualPropertyDefs();
		verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	
	@Test
	public void testUnsuccessfulNoMatches() throws ServletException, IOException, NoSuchNodeTypeException, RepositoryException {
		when(request.getRequestURI()).thenReturn("/ns:ntName/binPropDef/binary/true/true/true/true/ignore/default_binary_value.bin");
		invokeServletWithDifferentPropertyDefs();
		verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	

}
