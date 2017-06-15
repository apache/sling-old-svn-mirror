/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.core.impl;

import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "deprecation"})
public class ScriptingResourceResolverTest {

    private ScriptingResourceResolver scriptingResourceResolver;
    private ResourceResolver delegate;
    private ResourceResolver delegateClone;
    private static final String[] searchPaths = {"/apps", "/libs"};

    @Before
    public void setUpTestSuite() throws LoginException {
        delegate = mock(ResourceResolver.class);
        delegateClone = mock(ResourceResolver.class);
        when(delegate.clone(null)).thenReturn(delegateClone);
        when(delegateClone.getSearchPath()).thenReturn(searchPaths);
        scriptingResourceResolver = new ScriptingResourceResolver(false, delegate);
    }

    @Test
    public void testClose() throws Exception {
        scriptingResourceResolver.close();
        verify(delegate, times(0)).close();
    }

    @Test
    public void test_close() throws Exception {
        scriptingResourceResolver._close();
        verify(delegate).close();
    }

    @Test
    public void testClone() throws Exception {
        final Map<String, Object> authenticationInfo = mock(Map.class);
        final ResourceResolver result = scriptingResourceResolver.clone(authenticationInfo);
        assertTrue(result instanceof ScriptingResourceResolver);
        final ScriptingResourceResolver resultS = (ScriptingResourceResolver) result;
        assertArrayEquals(searchPaths, resultS.getSearchPath());
        verify(delegateClone).getSearchPath();
    }

}
