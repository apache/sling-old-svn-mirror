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
package org.apache.sling.ide.transport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.jcr.nodetype.NodeType;

import org.junit.Test;

public class FallbackNodeTypeRegistryTest {

    @Test
    public void nodesuperTypesAreFound() {
        FallbackNodeTypeRegistry registry = FallbackNodeTypeRegistry.createRegistryWithDefaultNodeTypes();

        NodeType nodeType = registry.getNodeType("sling:Folder");

        assertThat("nodeType", nodeType, notNullValue());
        assertThat("nodeType.name", nodeType.getName(), equalTo("sling:Folder"));

        assertThat("nodeType.declaredSupertypeNames", nodeType.getDeclaredSupertypeNames(),
                equalTo(new String[] { "nt:folder" }));

        NodeType[] superTypes = nodeType.getSupertypes();

        assertThat("nodeType.superTypes", superTypes, notNullValue());
        assertThat("nodeType.superTypes.length", superTypes.length, equalTo(1));
        assertThat("nodeType.superTypes[0].name", superTypes[0].getName(), equalTo("nt:folder"));
    }
}
