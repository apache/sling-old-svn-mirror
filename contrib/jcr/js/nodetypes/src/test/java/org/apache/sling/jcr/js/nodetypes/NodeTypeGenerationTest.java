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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import javax.servlet.ServletException;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.js.nodetypes.mock.MockNodeTypeGenerator;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests if the servlet generates the combinations node type definitions in the correct JSON format.
 */
public class NodeTypeGenerationTest extends MockNodeTypeGenerator{
	
	@Before
	public void setUp() throws RepositoryException, IOException{
		super.setUp();
	}

	@Test
	public void testSupertypeList() throws JSONException, ServletException, IOException{
		NodeType nt1 = getSimpleNodeTypeWithName("testNodeType");
		NodeType[] superTypes = {getSimpleNodeTypeWithName("superType1"), getSimpleNodeTypeWithName("superType2"), getSimpleNodeTypeWithName("superType3")};
		when(nt1.getDeclaredSupertypes()).thenReturn(superTypes);
		when(nodeTypeIterator.nextNodeType()).thenReturn(nt1);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testSupertypeList");
	}

	@Test
	public void testOneSimpleNodeType() throws ServletException, IOException, JSONException {
		NodeType nt1 = getSimpleNodeTypeWithName("testNodeType");
		when(nodeTypeIterator.nextNodeType()).thenReturn(nt1);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testOneSimpleNodeType");
	}

	@Test
	public void testSimpleNodeTypes() throws ServletException, IOException, JSONException {
		NodeType nt1 = getSimpleNodeTypeWithName("testNodeType1");
		NodeType nt2 = getSimpleNodeTypeWithName("testNodeType2");
		when(nodeTypeIterator.nextNodeType()).thenReturn(nt1, nt2);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testSimpleNodeTypes");
	}

	@Test
	public void testNodeTypeWithEmptyName() throws ServletException, IOException, JSONException {
		NodeType nt1 = getSimpleNodeTypeWithName("testNodeType1");
		NodeType nt2 = getSimpleNodeTypeWithName(null);
		when(nodeTypeIterator.nextNodeType()).thenReturn(nt1, nt2);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testNodeTypeWithEmptyName");
	}

	@Test
	public void testCompleteNodeTypes() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		NodeType nt1 = getSimpleNodeTypeWithName("testNodeType");
		NodeType[] superTypes = {getSimpleNodeTypeWithName("superType1"), getSimpleNodeTypeWithName("superType2"), getSimpleNodeTypeWithName("superType3")};
		NodeType nt2 = getSimpleNodeTypeWithName(null);
		when(nt1.getDeclaredSupertypes()).thenReturn(superTypes);
		when(nodeTypeIterator.nextNodeType()).thenReturn(nt1, nt2);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);


		NodeDefinition childNodeDef1 = getCompleteChildNodeDef("childNodeDef1");
		NodeDefinition childNodeDef2 = getCompleteChildNodeDef("childNodeDef2");
		
		NodeDefinition[] childNodeDefs = {childNodeDef1, childNodeDef2};
		when(nt1.getDeclaredChildNodeDefinitions()).thenReturn(childNodeDefs);

		String propertyName = "stringPropertyDef";
		PropertyDefinition propertyDef = mock(PropertyDefinition.class);
		when(propertyDef.getOnParentVersion()).thenReturn(OnParentVersionAction.VERSION);
		when(propertyDef.getName()).thenReturn(propertyName);
		when(propertyDef.getRequiredType()).thenReturn(PropertyType.STRING);
		when(propertyDef.getValueConstraints()).thenReturn( new String[]{GenerationConstants.CONSTRAINT_STRING});
		when(propertyDef.isMultiple()).thenReturn(Boolean.TRUE);
		when(propertyDef.isProtected()).thenReturn(Boolean.TRUE);
		Value defaultValue = mock(Value.class);
		when(defaultValue.getType()).thenReturn(PropertyType.STRING);
		when(defaultValue.getString()).thenReturn(GenerationConstants.DEFAULT_VALUE_STRING);
		when(propertyDef.getDefaultValues()).thenReturn(new Value[]{defaultValue});
		when(propertyDef.isAutoCreated()).thenReturn(Boolean.TRUE);
		when(propertyDef.isMandatory()).thenReturn(Boolean.TRUE);
		
		when(nt1.getDeclaredPropertyDefinitions()).thenReturn(new PropertyDefinition[]{propertyDef});

		assertEqualsWithServletResult("testCompleteNodeTypes");
	}

	public void testCompletePropertyDefinition(PropertyDefinition[] propertyDef) throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		NodeType ntWithChildNodeDefs = getSimpleNodeTypeWithName("ntWithPropertyDefs");

		when(ntWithChildNodeDefs.getDeclaredPropertyDefinitions()).thenReturn(propertyDef);

		when(nodeTypeIterator.nextNodeType()).thenReturn(ntWithChildNodeDefs);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testCompletePropertyDefinition");
	}
	
}
