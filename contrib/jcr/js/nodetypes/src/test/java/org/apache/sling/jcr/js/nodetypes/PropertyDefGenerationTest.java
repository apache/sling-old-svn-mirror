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

import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_STRING;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.DEFAULT_VALUE_STRING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.servlet.ServletException;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.js.nodetypes.mock.MockNodeTypeGenerator;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests if the servlet generates the combinations property definitions in the correct JSON format.
 */
public class PropertyDefGenerationTest extends MockNodeTypeGenerator{

	@Before
	public void setUp() throws RepositoryException, IOException{
		super.setUp();
	}

	@Test
	public void testCompleteBinaryPropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "stringPropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getCompleteBinaryPropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testCompleteBinaryPropertyDefinition");
	}

	@Test
	public void testCompleteStringPropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "stringPropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getCompleteStringPropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testCompleteStringPropertyDefinition");
	}

	@Test
	public void testCompleteDatePropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "datePropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getCompleteDatePropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testCompleteDatePropertyDefinition");
	}

	@Test
	public void testCompleteDoublePropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "doublePropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getCompleteDoublePropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testCompleteDoublePropertyDefinition");
	}

	@Test
	public void testCompleteLongPropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "longPropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getCompleteLongPropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testCompleteLongPropertyDefinition");
	}

	@Test
	public void testCompleteBooleanPropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "booleanPropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getCompleteBooleanPropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testCompleteBooleanPropertyDefinition");
	}

	@Test
	public void testCompleteNamePropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "namePropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getCompleteNamePropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testCompleteNamePropertyDefinition");
	}

	@Test
	public void testCompletePathPropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "pathPropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getCompletePathPropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testCompletePathPropertyDefinition");
	}

	@Test
	public void testCompleteReferencePropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "referencePropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getCompleteReferencePropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testCompleteReferencePropertyDefinition");
	}

	public void testCompletePropertyDefinition(PropertyDefinition[] propertyDef, String filename) throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		NodeType ntWithChildNodeDefs = getSimpleNodeTypeWithName("ntWithPropertyDefs");

		when(ntWithChildNodeDefs.getDeclaredPropertyDefinitions()).thenReturn(propertyDef);

		when(nodeTypeIterator.nextNodeType()).thenReturn(ntWithChildNodeDefs);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult(filename);
	}


	@Test
	public void testOneSimplePropertyDefinition() throws JSONException, ServletException, IOException, ValueFormatException, IllegalStateException, RepositoryException{
		String propertyName = "simplePropertyDef";
		PropertyDefinition[] propertyDef = {getPropertyGenerator().getSimplePropertyDef(propertyName)};
		testCompletePropertyDefinition(propertyDef, "testOneSimplePropertyDefinition");
	}

	@Test
	public void testTwoResidualPropertyDefinitions() throws ValueFormatException, IllegalStateException, RepositoryException, JSONException, ServletException, IOException{
		//Is only be possible for multi valued property defs
		PropertyDefinition[] residualPropertyDefs = {getPropertyGenerator().getCompleteStringPropertyDef("*"), getPropertyGenerator().getCompleteDatePropertyDef("*")};

		NodeType ntWithChildNodeDefs = getSimpleNodeTypeWithName("ntWithPropertyDefs");

		when(ntWithChildNodeDefs.getDeclaredPropertyDefinitions()).thenReturn(residualPropertyDefs);

		when(nodeTypeIterator.nextNodeType()).thenReturn(ntWithChildNodeDefs);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testTwoResidualPropertyDefinitions");
	}

	@Test
	public void testMultipleDefaultValues() throws ValueFormatException, IllegalStateException, RepositoryException, JSONException, ServletException, IOException{
		Value defaultValue1 = mock(Value.class);
		when(defaultValue1.getString()).thenReturn(DEFAULT_VALUE_STRING);
		Value defaultValue2 = mock(Value.class);
		when(defaultValue2.getString()).thenReturn(DEFAULT_VALUE_STRING+"2");
		PropertyDefinition propertyDef = getPropertyGenerator().getPropertyDef("stringProp", PropertyType.STRING, new String[]{CONSTRAINT_STRING}, new Value[] {defaultValue1, defaultValue2});

		NodeType ntWithChildNodeDefs = getSimpleNodeTypeWithName("ntWithPropertyDefs");

		when(ntWithChildNodeDefs.getDeclaredPropertyDefinitions()).thenReturn(new PropertyDefinition[]{propertyDef});

		when(nodeTypeIterator.nextNodeType()).thenReturn(ntWithChildNodeDefs);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult("testMultipleDefaultValues");
	}

	@Test
	public void testMultipleConstraints() throws ValueFormatException, IllegalStateException, RepositoryException, JSONException, ServletException, IOException{
		PropertyDefinition propertyDef = getPropertyGenerator().getPropertyDef("stringProp", PropertyType.STRING, new String[]{"banana", "apple"}, null);

		NodeType ntWithChildNodeDefs = getSimpleNodeTypeWithName("ntWithPropertyDefs");

		when(ntWithChildNodeDefs.getDeclaredPropertyDefinitions()).thenReturn(new PropertyDefinition[]{propertyDef});

		when(nodeTypeIterator.nextNodeType()).thenReturn(ntWithChildNodeDefs);
		when(nodeTypeIterator.hasNext()).thenReturn(Boolean.TRUE, Boolean.FALSE);
		
		assertEqualsWithServletResult( "testMultipleConstraints");
	}
}
