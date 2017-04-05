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
package org.apache.sling.jcr.js.nodetypes;

import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.servlet.ServletException;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.js.nodetypes.mock.MockNodeTypeGenerator;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests if the servlet generates the combinations child node definitions in the correct JSON format. 
 */
public class ChildNodeDefGenerationTest extends MockNodeTypeGenerator{

	@Before
	public void setUp() throws RepositoryException, IOException{
		super.setUp();
	}
	
	@Test
	public void testOneSimpleChildNodeDefinition() throws ServletException, IOException, JSONException{
		NodeType ntWithChildNodeDefs = getSimpleNodeTypeWithName("ntWithChildNodeDefs");

		NodeDefinition[] childNodeDefs = {getSimpleChildNodeDef("childNodeDef1")};
		when(ntWithChildNodeDefs.getDeclaredChildNodeDefinitions()).thenReturn(childNodeDefs);

		when(nodeTypeIterator.nextNodeType()).thenReturn(ntWithChildNodeDefs);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testOneSimpleChildNodeDefinition");
	}
	
	@Test
	public void testSimpleChildNodeDefinitions() throws ServletException, IOException, JSONException{
		NodeType ntWithChildNodeDefs = getSimpleNodeTypeWithName("ntWithChildNodeDefs");

		NodeDefinition childNodeDef1 = getSimpleChildNodeDef("childNodeDef1");
		NodeDefinition childNodeDef2 = getSimpleChildNodeDef("childNodeDef2");
		
		NodeDefinition[] childNodeDefs = {childNodeDef1, childNodeDef2};
		when(ntWithChildNodeDefs.getDeclaredChildNodeDefinitions()).thenReturn(childNodeDefs);

		when(nodeTypeIterator.nextNodeType()).thenReturn(ntWithChildNodeDefs);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testSimpleChildNodeDefinitions");
	}
	
	@Test
	public void testCompleteChildNodeDefinitions() throws ServletException, IOException, JSONException{
		NodeType ntWithChildNodeDefs = getSimpleNodeTypeWithName("ntWithChildNodeDefs");

		NodeDefinition childNodeDef1 = getCompleteChildNodeDef("childNodeDef1");
		NodeDefinition childNodeDef2 = getCompleteChildNodeDef("childNodeDef2");
		
		NodeDefinition[] childNodeDefs = {childNodeDef1, childNodeDef2};
		when(ntWithChildNodeDefs.getDeclaredChildNodeDefinitions()).thenReturn(childNodeDefs);

		when(nodeTypeIterator.nextNodeType()).thenReturn(ntWithChildNodeDefs);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testCompleteChildNodeDefinitions");
	}
	
	@Test
	public void testResidualChildNodeDefinitions() throws JSONException, ServletException, IOException{
		NodeType ntWithChildNOdeDefs = getSimpleNodeTypeWithName("ntWithChildNodeDefs");

		NodeDefinition childNodeDef1 = getSimpleChildNodeDef("*");
		NodeDefinition childNodeDef2 = getSimpleChildNodeDef("*");
		NodeDefinition childNodeDef3 = getSimpleChildNodeDef("childNodeDef");
		
		NodeDefinition[] childNodeDefs = {childNodeDef1, childNodeDef2, childNodeDef3};
		when(ntWithChildNOdeDefs.getDeclaredChildNodeDefinitions()).thenReturn(childNodeDefs);

		when(nodeTypeIterator.nextNodeType()).thenReturn(ntWithChildNOdeDefs);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testResidualChildNodeDefinitions");
	}

}
