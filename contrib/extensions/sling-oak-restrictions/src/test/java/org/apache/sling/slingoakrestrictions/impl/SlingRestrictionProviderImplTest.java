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
package org.apache.sling.slingoakrestrictions.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.Restriction;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionDefinition;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionPattern;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class SlingRestrictionProviderImplTest {
    
    private static final String TEST_PATH = "/content/path/to/test";

    private static final String RESOURCE_TYPE1 = "myproj/comp1";
    private static final String RESOURCE_TYPE2 = "myproj/comp2";
    
    @Mock
    private Tree restrictionNodeTree;

    @Mock
    private PropertyState restrictionProperty;
    
    @Mock
    private Restriction restriction;
    
    @Mock
    private RestrictionDefinition definition;
    
    private SlingRestrictionProviderImpl slingRestrictionProviderImpl;

    @Before
    public void setup() {
        initMocks(this);
        
        doReturn(definition).when(restriction).getDefinition();
        doReturn(restrictionProperty).when(restriction).getProperty();
    }
    
    @Test
    public void testGetPatternFromTreeResourceTypes() {
        
        doReturn(restrictionProperty).when(restrictionNodeTree).getProperty(SlingRestrictionProviderImpl.REP_RESOURCE_TYPES);
        doReturn(Arrays.asList(RESOURCE_TYPE1, RESOURCE_TYPE2)).when(restrictionProperty).getValue(Type.STRINGS);
        
        slingRestrictionProviderImpl = new SlingRestrictionProviderImpl();
        
        RestrictionPattern pattern = slingRestrictionProviderImpl.getPattern(TEST_PATH, restrictionNodeTree);
        assertTrue(pattern instanceof ResourceTypePattern);
        ResourceTypePattern resourceTypePattern = (ResourceTypePattern) pattern;
        
        assertFalse(resourceTypePattern.isMatchChildren());
        assertEquals(TEST_PATH, resourceTypePattern.getLimitedToPath());
    }

    @Test
    public void testGetPatternFromTreeResourceTypesWithChildren() {
        
        doReturn(restrictionProperty).when(restrictionNodeTree).getProperty(SlingRestrictionProviderImpl.REP_RESOURCE_TYPES_WITH_CHILDREN);
        doReturn(Arrays.asList(RESOURCE_TYPE1, RESOURCE_TYPE2)).when(restrictionProperty).getValue(Type.STRINGS);
        
        slingRestrictionProviderImpl = new SlingRestrictionProviderImpl();
        
        RestrictionPattern pattern = slingRestrictionProviderImpl.getPattern(TEST_PATH, restrictionNodeTree);
        assertTrue(pattern instanceof ResourceTypePattern);
        ResourceTypePattern resourceTypePattern = (ResourceTypePattern) pattern;
        
        assertTrue(resourceTypePattern.isMatchChildren());
        assertEquals(TEST_PATH, resourceTypePattern.getLimitedToPath());
    }
    
    @Test
    public void testGetPatternFromRestrictionsResourceTypes() {
        
        doReturn(SlingRestrictionProviderImpl.REP_RESOURCE_TYPES).when(definition).getName();
        doReturn(Arrays.asList(RESOURCE_TYPE1, RESOURCE_TYPE2)).when(restrictionProperty).getValue(Type.STRINGS);
        
        slingRestrictionProviderImpl = new SlingRestrictionProviderImpl();
        
        RestrictionPattern pattern = slingRestrictionProviderImpl.getPattern(TEST_PATH, new HashSet<Restriction>(Arrays.asList(restriction)));
        assertTrue(pattern instanceof ResourceTypePattern);
        ResourceTypePattern resourceTypePattern = (ResourceTypePattern) pattern;
        
        assertFalse(resourceTypePattern.isMatchChildren());
        assertEquals(TEST_PATH, resourceTypePattern.getLimitedToPath());

    }

    @Test
    public void testGetPatternFromRestrictionsResourceTypesWithChildren() {
        
        doReturn(SlingRestrictionProviderImpl.REP_RESOURCE_TYPES_WITH_CHILDREN).when(definition).getName();
        doReturn(Arrays.asList(RESOURCE_TYPE1, RESOURCE_TYPE2)).when(restrictionProperty).getValue(Type.STRINGS);
        
        slingRestrictionProviderImpl = new SlingRestrictionProviderImpl();
        
        RestrictionPattern pattern = slingRestrictionProviderImpl.getPattern(TEST_PATH, new HashSet<Restriction>(Arrays.asList(restriction)));
        assertTrue(pattern instanceof ResourceTypePattern);
        ResourceTypePattern resourceTypePattern = (ResourceTypePattern) pattern;
        
        assertTrue(resourceTypePattern.isMatchChildren());
        assertEquals(TEST_PATH, resourceTypePattern.getLimitedToPath());

    }
    
}
