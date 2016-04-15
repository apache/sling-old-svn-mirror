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

import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_BINARY;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_BOOLEAN;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_DATE;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_DOUBLE;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_LONG;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_NAME;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_PATH;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_REFERENCE;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.CONSTRAINT_STRING;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.DEFAULT_VALUE_BOOLEAN;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.DEFAULT_VALUE_CALENDAR;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.DEFAULT_VALUE_DOUBLE;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.DEFAULT_VALUE_LONG;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.DEFAULT_VALUE_NAME;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.DEFAULT_VALUE_PATH;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.DEFAULT_VALUE_REFERENCE;
import static org.apache.sling.jcr.js.nodetypes.GenerationConstants.DEFAULT_VALUE_STRING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.js.nodetypes.GenerationConstants;

/**
 * Generates the PropertyDefinition mocks to simulate the property definitions that the servlet gets
 * from the node type manager.
 *
 */
public class MockPropertyDefGenerator {

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
	
	public PropertyDefinition getSimplePropertyDef(String name) {
		PropertyDefinition propertyDef = mock(PropertyDefinition.class);
		when(propertyDef.getOnParentVersion()).thenReturn(OnParentVersionAction.COPY);
		when(propertyDef.getName()).thenReturn(name);
		return propertyDef;
	}	

	private PropertyDefinition getCompletePropertyDev(String name, Integer type, String[] valueConstraints, String defaultValueString) throws ValueFormatException, RepositoryException {
		Value defaultValue = mock(Value.class);
		when(defaultValue.getString()).thenReturn(defaultValueString);
		PropertyDefinition propertyDef = getPropertyDef(name, type, valueConstraints, new Value[] {defaultValue});
		return propertyDef;
	}

	/**
	 * @param name
	 * @param type
	 * @param valueConstraints
	 * @param defaultValue - the getType() method will be mocked within this method. You only need to mock the getString(), getDouble() methods.
	 * @return
	 * @throws ValueFormatException
	 * @throws RepositoryException
	 */
	public PropertyDefinition getPropertyDef(String name, Integer type, String[] valueConstraints, Value[] defaultValues)
			throws ValueFormatException, RepositoryException {
		return getPropertyDef(name, type, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, OnParentVersionAction.VERSION, valueConstraints, defaultValues);
	}

	public PropertyDefinition getPropertyDef(String propertyName, Integer type, Boolean isAutoCreated, Boolean isMandatory, Boolean isProtected, Boolean isMultiple, Integer onParentVersionAction, String[] valueConstraints, Value[] defaultValues)
			throws ValueFormatException, RepositoryException {
		PropertyDefinition propertyDef = mock(PropertyDefinition.class);
		when(propertyDef.getOnParentVersion()).thenReturn(onParentVersionAction);
		when(propertyDef.getName()).thenReturn(propertyName);
		when(propertyDef.getRequiredType()).thenReturn(type);
		when(propertyDef.getValueConstraints()).thenReturn(valueConstraints);
		when(propertyDef.isMultiple()).thenReturn(isMultiple);
		when(propertyDef.isProtected()).thenReturn(isProtected);
		NodeType declaringNodeType = mock(NodeType.class);
		when(declaringNodeType.getName()).thenReturn("ntName");
		when(propertyDef.getDeclaringNodeType()).thenReturn(declaringNodeType);
		if (defaultValues!=null){
			for (Value defaultValue : defaultValues) {
				when(defaultValue.getType()).thenReturn(type);
			}
		}
		when(propertyDef.getDefaultValues()).thenReturn(defaultValues);
		when(propertyDef.isAutoCreated()).thenReturn(isAutoCreated);
		when(propertyDef.isMandatory()).thenReturn(isMandatory);
		return propertyDef;
	}
	
	@SuppressWarnings("deprecation")
	public PropertyDefinition[] getDifferentPropertyDefinitions() throws ValueFormatException, RepositoryException{
		Value defaultValue1 = mock(Value.class);
		when(defaultValue1.getType()).thenReturn(PropertyType.BINARY);
		when(defaultValue1.getStream()).thenReturn(new ByteArrayInputStream(GenerationConstants.DEFAULT_VALUE_BINARY_0.getBytes()));
		Value defaultValue2 = mock(Value.class);
		when(defaultValue2.getType()).thenReturn(PropertyType.BINARY);
		when(defaultValue2.getStream()).thenReturn(new ByteArrayInputStream(GenerationConstants.DEFAULT_VALUE_BINARY_1.getBytes()));

		PropertyDefinition binPropDef1 = this.getPropertyDef("binPropDef", PropertyType.BINARY, true, true, true, true, OnParentVersionAction.VERSION, new String[]{"[,1024]", "[,2048]"}, new Value[] {defaultValue1, defaultValue2});
		PropertyDefinition binPropDef2 = this.getPropertyDef("binPropDef", PropertyType.BINARY, true, true, true, true, OnParentVersionAction.COPY, new String[]{"[,1024]", "[,512]"}, new Value[] {defaultValue1, defaultValue2});
		PropertyDefinition binPropDef3 = this.getPropertyDef("binPropDef", PropertyType.BINARY, true, true, true, false, OnParentVersionAction.COPY, new String[]{"[,1024]", "[,512]"}, new Value[] {defaultValue1, defaultValue2});
		PropertyDefinition binPropDef4 = this.getPropertyDef("binPropDef", PropertyType.BINARY, true, true, false, false, OnParentVersionAction.COPY, new String[]{"[,1024]", "[,512]"}, new Value[] {defaultValue1, defaultValue2});
		PropertyDefinition binPropDef5 = this.getPropertyDef("binPropDef", PropertyType.BINARY, true, false, false, false, OnParentVersionAction.COPY, new String[]{"[,1024]", "[,512]"}, new Value[] {defaultValue1, defaultValue2});
		PropertyDefinition binPropDef6 = this.getPropertyDef("binPropDef", PropertyType.BINARY, false, false, false, false, OnParentVersionAction.COPY, new String[]{"[,1024]", "[,512]"}, new Value[] {defaultValue1, defaultValue2});
		PropertyDefinition binPropDef7 = this.getPropertyDef("binPropDef", PropertyType.BOOLEAN, false, false, false, false, OnParentVersionAction.COPY, new String[]{"[,1024]", "[,512]"}, new Value[] {defaultValue1, defaultValue2});
		PropertyDefinition binPropDef8 = this.getPropertyDef("binPropDef2", PropertyType.BOOLEAN, false, false, false, false, OnParentVersionAction.COPY, new String[]{"[,1024]", "[,512]"}, new Value[] {defaultValue1, defaultValue2});
		return new PropertyDefinition[]{binPropDef8, binPropDef7, binPropDef6, binPropDef5, binPropDef4, binPropDef3, binPropDef2, binPropDef1};
	}
	
	@SuppressWarnings("deprecation")
	public PropertyDefinition[] getEqualPropertyDefinitions() throws ValueFormatException, RepositoryException{
		Value defaultValue1 = mock(Value.class);
		when(defaultValue1.getType()).thenReturn(PropertyType.BINARY);
		when(defaultValue1.getStream()).thenReturn(new ByteArrayInputStream("A content".getBytes()));
		Value defaultValue2 = mock(Value.class);
		when(defaultValue2.getType()).thenReturn(PropertyType.BINARY);
		when(defaultValue2.getStream()).thenReturn(new ByteArrayInputStream("An other content".getBytes()));

		PropertyDefinition binPropDef1 = this.getPropertyDef("binPropDef", PropertyType.BINARY, true, true, true, true, OnParentVersionAction.VERSION, new String[]{"[,1024]", "[,2048]"}, new Value[] {defaultValue1, defaultValue2});
		PropertyDefinition binPropDef2 = this.getPropertyDef("binPropDef", PropertyType.BINARY, true, true, true, true, OnParentVersionAction.VERSION, new String[]{"[,1024]", "[,512]"}, new Value[] {defaultValue1, defaultValue2});
		return new PropertyDefinition[]{binPropDef2, binPropDef1};
	}
	
	public PropertyDefinition getCompleteStringPropertyDef(String name) throws ValueFormatException, IllegalStateException, RepositoryException {
		return getCompletePropertyDev(name, PropertyType.STRING, new String[]{CONSTRAINT_STRING}, DEFAULT_VALUE_STRING);
	}
	
	@SuppressWarnings("deprecation")
	public PropertyDefinition getCompleteBinaryPropertyDef(String name) throws ValueFormatException, IllegalStateException, RepositoryException {
		Value defaultValue = mock(Value.class);
		when(defaultValue.getType()).thenReturn(PropertyType.BINARY);
		when(defaultValue.getStream()).thenReturn(new ByteArrayInputStream("A content".getBytes()));
		return getPropertyDef(name, PropertyType.BINARY, new String[]{CONSTRAINT_BINARY}, new Value[] {defaultValue});
	}
	
	public PropertyDefinition getCompleteDatePropertyDef(String name) throws ValueFormatException, IllegalStateException, RepositoryException {
		Value defaultValue = mock(Value.class);
		when(defaultValue.getDate()).thenReturn(DEFAULT_VALUE_CALENDAR);
		return getPropertyDef(name, PropertyType.DATE, new String[]{CONSTRAINT_DATE}, new Value[] {defaultValue});
	}

	public PropertyDefinition getCompleteDoublePropertyDef(String name) throws ValueFormatException, IllegalStateException, RepositoryException {
		Value defaultValue = mock(Value.class);
		when(defaultValue.getDouble()).thenReturn(DEFAULT_VALUE_DOUBLE);
		return getPropertyDef(name, PropertyType.DOUBLE, new String[]{CONSTRAINT_DOUBLE}, new Value[] {defaultValue});
	}
	
	public PropertyDefinition getCompleteLongPropertyDef(String name) throws ValueFormatException, IllegalStateException, RepositoryException {
		Value defaultValue = mock(Value.class);
		when(defaultValue.getLong()).thenReturn(DEFAULT_VALUE_LONG);
		return getPropertyDef(name, PropertyType.LONG, new String[]{CONSTRAINT_LONG}, new Value[] {defaultValue});
	}
	
	public PropertyDefinition getCompleteBooleanPropertyDef(String name) throws ValueFormatException, IllegalStateException, RepositoryException {
		Value defaultValue = mock(Value.class);
		when(defaultValue.getBoolean()).thenReturn(DEFAULT_VALUE_BOOLEAN);
		return getPropertyDef(name, PropertyType.BOOLEAN, new String[]{CONSTRAINT_BOOLEAN}, new Value[] {defaultValue});
	}
	
	public PropertyDefinition getCompleteNamePropertyDef(String name) throws ValueFormatException, IllegalStateException, RepositoryException {
		Value defaultValue = mock(Value.class);
		when(defaultValue.getString()).thenReturn(DEFAULT_VALUE_NAME);
		return getPropertyDef(name, PropertyType.NAME, new String[]{CONSTRAINT_NAME}, new Value[] {defaultValue});
	}
	
	public PropertyDefinition getCompletePathPropertyDef(String name) throws ValueFormatException, IllegalStateException, RepositoryException {
		Value defaultValue = mock(Value.class);
		when(defaultValue.getString()).thenReturn(DEFAULT_VALUE_PATH);
		return getPropertyDef(name, PropertyType.PATH, new String[]{CONSTRAINT_PATH}, new Value[] {defaultValue});
	}
	
	public PropertyDefinition getCompleteReferencePropertyDef(String name) throws ValueFormatException, IllegalStateException, RepositoryException {
		Value defaultValue = mock(Value.class);
		when(defaultValue.getString()).thenReturn(DEFAULT_VALUE_REFERENCE);
		return getPropertyDef(name, PropertyType.REFERENCE, new String[]{CONSTRAINT_REFERENCE}, new Value[] {defaultValue});
	}
	
}
