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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class ResourceTypePatternTest {
    
    private static final String TEST_PATH = "/content/path/to/test";
    
    private static final String RESOURCE_TYPE_TEST_PATH = "myproj/comp1";
    private static final String RESOURCE_TYPE_SUB1 = "myproj/comp2";
    private static final String RESOURCE_TYPE_SUB2 = "myproj/comp3";
    private static final String RESOURCE_TYPE_SUB3 = "myproj/comp4";
    private static final String RESOURCE_TYPE_SUBSUB1 = "myproj/comp5";
    
    private static final String RESOURCE_TYPE_OUTSIDE_SCOPE = "myproj/outsidescope";
    
    private static final String TEST_NODE_SUB1 = "subfolder1";        
    private static final String TEST_NODE_SUB2 = "subfolder2";
    private static final String TEST_NODE_SUB3 = "subfolder3";
    private static final String TEST_NODE_SUBSUB1 = "subsubfolder1";
    private static final String TEST_NODE_SUBSUB2 = "subsubfolder2";

    @Mock
    private Tree testTree;
    
    @Mock
    private Tree testTreeSub1;    
   
    @Mock
    private Tree testTreeSub2;

    @Mock
    private Tree testTreeSub3;
    
    @Mock
    private Tree testTreeSub3Sub1;
    
    @Mock
    private Tree testTreeSub3Sub2;
    
    @Mock
    private Tree testTreeParentOutsideScope;
    
    
    private ResourceTypePattern resourceTypePattern;
    

    @Before
    public void setup() {
        initMocks(this);
        
        setupTreeMock(testTreeParentOutsideScope, StringUtils.substringBeforeLast(TEST_PATH, "/"), null, RESOURCE_TYPE_OUTSIDE_SCOPE);
        setupTreeMock(testTree, StringUtils.substringAfterLast(TEST_PATH, "/"), testTreeParentOutsideScope, RESOURCE_TYPE_TEST_PATH);

        setupTreeMock(testTreeSub1, TEST_NODE_SUB1, testTree, RESOURCE_TYPE_SUB1);
        setupTreeMock(testTreeSub2, TEST_NODE_SUB2, testTree, RESOURCE_TYPE_SUB2);
        setupTreeMock(testTreeSub3, TEST_NODE_SUB3, testTree, RESOURCE_TYPE_SUB3);

        setupTreeMock(testTreeSub3Sub1, TEST_NODE_SUBSUB1, testTreeSub3, RESOURCE_TYPE_SUBSUB1);
        setupTreeMock(testTreeSub3Sub2, TEST_NODE_SUBSUB2, testTreeSub3, RESOURCE_TYPE_SUBSUB1);
        
    }



    @Test
    public void testBasicMatchWithoutChildren() {
        
        resourceTypePattern = new ResourceTypePattern(Arrays.asList(RESOURCE_TYPE_TEST_PATH, RESOURCE_TYPE_SUB1, RESOURCE_TYPE_SUB3), TEST_PATH, false);
        
        assertNonTreeFunctionsReturnFalse();
        
        assertTrue(resourceTypePattern.matches(testTree, null));
        assertTrue(resourceTypePattern.matches(testTreeSub1, null));
        assertFalse(resourceTypePattern.matches(testTreeSub2, null));
        assertTrue(resourceTypePattern.matches(testTreeSub3, null));
        assertFalse(resourceTypePattern.matches(testTreeSub3Sub1, null));
        assertFalse(resourceTypePattern.matches(testTreeSub3Sub2, null));

    }
    
    @Test
    public void testMatchWithoutChildren() {
        
        String restrictionWithPath = RESOURCE_TYPE_SUB2+"@"+TEST_NODE_SUB2;
        resourceTypePattern = new ResourceTypePattern(Arrays.asList(restrictionWithPath, RESOURCE_TYPE_SUB3), TEST_PATH, false);
        
        assertNonTreeFunctionsReturnFalse();
        
        assertTrue("Has to match because of @path usage "+restrictionWithPath, resourceTypePattern.matches(testTree, null));
        assertFalse(resourceTypePattern.matches(testTreeSub1, null));
        assertFalse("Must not match (although it has "+RESOURCE_TYPE_SUB2+", it does not have a child '"+TEST_NODE_SUB2+"' with this resource type)", resourceTypePattern.matches(testTreeSub2, null));
        assertTrue("Has to match because of "+RESOURCE_TYPE_SUB3+" in list", resourceTypePattern.matches(testTreeSub3, null));
        assertFalse(resourceTypePattern.matches(testTreeSub3Sub1, null));
        assertFalse(resourceTypePattern.matches(testTreeSub3Sub2, null));
    }
    
    @Test
    public void testMatchWithChildren() {
        
        resourceTypePattern = new ResourceTypePattern(Arrays.asList(RESOURCE_TYPE_SUB3), TEST_PATH, true);
        
        assertNonTreeFunctionsReturnFalse();
        
        assertFalse("Not a node at or below "+RESOURCE_TYPE_SUB3, resourceTypePattern.matches(testTree, null));
        assertFalse("Not a node at or below "+RESOURCE_TYPE_SUB3, resourceTypePattern.matches(testTreeSub1, null));
        assertFalse("Not a node at or below "+RESOURCE_TYPE_SUB3, resourceTypePattern.matches(testTreeSub2, null));
        assertTrue("The node with "+RESOURCE_TYPE_SUB3, resourceTypePattern.matches(testTreeSub3, null));
        assertTrue("A node below a node with "+RESOURCE_TYPE_SUB3, resourceTypePattern.matches(testTreeSub3Sub1, null));
        assertTrue("A node below a node with "+RESOURCE_TYPE_SUB3, resourceTypePattern.matches(testTreeSub3Sub2, null));
    }
    
    @Test
    public void testMatchWithChildrenDoesNotLeaveBasePath() {
        
        resourceTypePattern = new ResourceTypePattern(Arrays.asList(RESOURCE_TYPE_OUTSIDE_SCOPE), TEST_PATH, true);
        
        assertNonTreeFunctionsReturnFalse();
        
        // all false as RESOURCE_TYPE_OUTSIDE_SCOPE is only found above TEST_PATH
        assertFalse(resourceTypePattern.matches(testTree, null));
        assertFalse(resourceTypePattern.matches(testTreeSub1, null));
        assertFalse(resourceTypePattern.matches(testTreeSub2, null));
        assertFalse(resourceTypePattern.matches(testTreeSub3, null));
        assertFalse(resourceTypePattern.matches(testTreeSub3Sub1, null));
        assertFalse(resourceTypePattern.matches(testTreeSub3Sub2, null));
    }
    
    @Test
    public void testMatchWithChildrenAndPathUsage() {
        
        String restrictionWithPath = RESOURCE_TYPE_SUBSUB1+"@"+TEST_NODE_SUBSUB1;
        resourceTypePattern = new ResourceTypePattern(Arrays.asList(restrictionWithPath), TEST_PATH, true);
        
        assertNonTreeFunctionsReturnFalse();
        
        assertFalse("This node or any of its parents do not have a sub node '"+TEST_NODE_SUBSUB1+"' with "+RESOURCE_TYPE_SUBSUB1, resourceTypePattern.matches(testTree, null));
        assertFalse("This node or any of its parents do not have a sub node '"+TEST_NODE_SUBSUB1+"' with "+RESOURCE_TYPE_SUBSUB1, resourceTypePattern.matches(testTreeSub1, null));
        assertFalse("This node or any of its parents do not have a sub node '"+TEST_NODE_SUBSUB1+"' with "+RESOURCE_TYPE_SUBSUB1,resourceTypePattern.matches(testTreeSub2, null));
        assertTrue("This node does have a sub node '"+TEST_NODE_SUBSUB1+"' with "+RESOURCE_TYPE_SUBSUB1, resourceTypePattern.matches(testTreeSub3, null));
        assertTrue("A node in parent hierarchy does have a sub node '"+TEST_NODE_SUBSUB1+"' with "+RESOURCE_TYPE_SUBSUB1, resourceTypePattern.matches(testTreeSub3Sub1, null));
        assertTrue("A node in parent hierarchy does have a sub node '"+TEST_NODE_SUBSUB1+"' with "+RESOURCE_TYPE_SUBSUB1, resourceTypePattern.matches(testTreeSub3Sub2, null));
    }
    

    private void assertNonTreeFunctionsReturnFalse() {
        assertFalse(resourceTypePattern.matches(TEST_PATH));
        assertFalse(resourceTypePattern.matches());
    }

    private void setupTreeMock(Tree tree, String path, Tree parentTree, String resourceType) {

        doReturn(parentTree==null).when(tree).isRoot();
        doReturn(parentTree).when(tree).getParent();
        
        mockGetStringProperty(tree, ResourceTypePattern.SLING_RESOURCE_TYPE, resourceType);
        
        String effectivePath = path;
        if(parentTree!=null) {
            effectivePath = parentTree.getPath() + "/" + path;
            String childNodeName = StringUtils.substringAfterLast(effectivePath, "/");
            doReturn(tree).when(parentTree).getChild(childNodeName);
        }
        doReturn(effectivePath).when(tree).getPath();
        
        // default for getChild
        doThrow(new IllegalArgumentException()).when(tree).getChild(Mockito.anyString());
        
    }
    
    private void mockGetStringProperty(Tree tree, String propertyName, String value) {
        PropertyState propertyState = mock(PropertyState.class);
        doReturn(propertyState).when(tree).getProperty(propertyName);
        doReturn(false).when(propertyState).isArray();
        doReturn(value).when(propertyState).getValue(Type.STRING);
    }
}
