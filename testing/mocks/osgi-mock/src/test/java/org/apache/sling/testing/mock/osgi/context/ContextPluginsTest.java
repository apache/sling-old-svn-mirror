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
package org.apache.sling.testing.mock.osgi.context;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class ContextPluginsTest {
    
    private OsgiContext context = new OsgiContext();

    @Mock
    private ContextPlugin plugin1;
    @Mock
    private ContextPlugin plugin2;
    @Mock
    private ContextCallback callback1;
    @Mock
    private ContextCallback callback2;
    
    @Test
    public void testConstructorSetUp() throws Exception {
        ContextPlugins underTest = new ContextPlugins(callback1);
        
        assertEquals(1, underTest.getPlugins().size());
        
        underTest.executeAfterSetUpCallback(context);
        
        verify(callback1, times(1)).execute(context);
    }

    @Test
    public void testConstructorSetUpTearDown() throws Exception {
        ContextPlugins underTest = new ContextPlugins(callback1, callback2);
        
        assertEquals(2, underTest.getPlugins().size());
        
        underTest.executeAfterSetUpCallback(context);
        underTest.executeBeforeTearDownCallback(context);

        verify(callback1, times(1)).execute(context);
        verify(callback2, times(1)).execute(context);
    }

    @Test
    public void testExecuteBeforeSetUpCallback() throws Exception {
        ContextPlugins underTest = new ContextPlugins();
        underTest.addPlugin(plugin1, plugin2, null);
        
        assertEquals(2, underTest.getPlugins().size());
        
        underTest.executeBeforeSetUpCallback(context);
        verify(plugin1, times(1)).beforeSetUp(context);
        verify(plugin2, times(1)).beforeSetUp(context);
        verifyNoMoreInteractions(plugin1);
        verifyNoMoreInteractions(plugin2);
    }

    @Test
    public void testExecuteAfterSetUpCallback() throws Exception {
        ContextPlugins underTest = new ContextPlugins();
        underTest.addPlugin(plugin1, plugin2, null);
        
        assertEquals(2, underTest.getPlugins().size());
        
        underTest.executeAfterSetUpCallback(context);
        verify(plugin1, times(1)).afterSetUp(context);
        verify(plugin2, times(1)).afterSetUp(context);
        verifyNoMoreInteractions(plugin1);
        verifyNoMoreInteractions(plugin2);
    }

    @Test
    public void testExecuteBeforeTearDownCallback() throws Exception {
        ContextPlugins underTest = new ContextPlugins();
        underTest.addPlugin(plugin1, plugin2, null);
        
        assertEquals(2, underTest.getPlugins().size());
        
        underTest.executeBeforeTearDownCallback(context);
        verify(plugin1, times(1)).beforeTearDown(context);
        verify(plugin2, times(1)).beforeTearDown(context);
        verifyNoMoreInteractions(plugin1);
        verifyNoMoreInteractions(plugin2);
    }

    @Test
    public void testExecuteAfterTearDownCallback() throws Exception {
        ContextPlugins underTest = new ContextPlugins();
        underTest.addPlugin(plugin1, plugin2, null);
        
        assertEquals(2, underTest.getPlugins().size());
        
        underTest.executeAfterTearDownCallback(context);
        verify(plugin1, times(1)).afterTearDown(context);
        verify(plugin2, times(1)).afterTearDown(context);
        verifyNoMoreInteractions(plugin1);
        verifyNoMoreInteractions(plugin2);
    }

    @Test
    public void testAddBeforeSetUpCallback() throws Exception {
        ContextPlugins underTest = new ContextPlugins();
        underTest.addBeforeSetUpCallback(callback1, callback2, null);
        
        assertEquals(2, underTest.getPlugins().size());
        
        underTest.executeBeforeSetUpCallback(context);
        verify(callback1, times(1)).execute(context);
        verify(callback2, times(1)).execute(context);
    }

    @Test
    public void testAddAfterSetUpCallback() throws Exception {
        ContextPlugins underTest = new ContextPlugins();
        underTest.addAfterSetUpCallback(callback1, callback2, null);
        
        assertEquals(2, underTest.getPlugins().size());
        
        underTest.executeAfterSetUpCallback(context);
        verify(callback1, times(1)).execute(context);
        verify(callback2, times(1)).execute(context);
    }

    @Test
    public void testAddBeforeTearDownCallback() throws Exception {
        ContextPlugins underTest = new ContextPlugins();
        underTest.addBeforeTearDownCallback(callback1, callback2, null);
        
        assertEquals(2, underTest.getPlugins().size());
        
        underTest.executeBeforeTearDownCallback(context);
        verify(callback1, times(1)).execute(context);
        verify(callback2, times(1)).execute(context);
    }

    @Test
    public void testAddAfterTearDownCallback() throws Exception {
        ContextPlugins underTest = new ContextPlugins();
        underTest.addAfterTearDownCallback(callback1, callback2, null);
        
        assertEquals(2, underTest.getPlugins().size());
        
        underTest.executeAfterTearDownCallback(context);
        verify(callback1, times(1)).execute(context);
        verify(callback2, times(1)).execute(context);
    }

}
