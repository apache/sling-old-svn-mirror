/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderContextImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class RenderContextImplTest {

    private RenderContextImpl renderContext;

    @Mock
    private ResourceResolver resolver;

    @Before
    public void setUp() throws Exception {
        Bindings bindings = new SimpleBindings();
        Map<String, RuntimeExtension> extensionsMap = new HashMap<String, RuntimeExtension>();
        renderContext = new RenderContextImpl(bindings, extensionsMap, resolver);
    }

    @After
    public void tearDown() throws Exception {
        renderContext = null;
    }

    @Test
    public void testGetScriptResourceResolver() throws Exception {
        ResourceResolver scriptResolver = renderContext.getScriptResourceResolver();
        assertNotNull("Expected a non-null resource resolver.", scriptResolver);
    }

    @Test
    public void testGetCollectionWithOneElement() {
        String stringObject = "test";
        Integer numberObject = 1;
        Collection stringCollection = renderContext.toCollection(stringObject);
        assertTrue(stringCollection.size() == 1 && stringCollection.contains(stringObject));
        Collection numberCollection = renderContext.toCollection(numberObject);
        assertTrue(numberCollection.size() == 1 && numberCollection.contains(numberObject));
    }
}
