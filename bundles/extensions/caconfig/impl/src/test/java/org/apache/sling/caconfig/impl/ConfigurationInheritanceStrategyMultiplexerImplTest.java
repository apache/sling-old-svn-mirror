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
package org.apache.sling.caconfig.impl;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.spi.ConfigurationInheritanceStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationInheritanceStrategyMultiplexerImplTest {
    
    @Rule
    public SlingContext context = new SlingContext();

    @Mock
    private Resource resource1;
    @Mock
    private Resource resource2;
    
    private Iterator<Resource> resources;
    private ConfigurationInheritanceStrategy underTest;
    
    @Before
    public void setUp() {
        resources = ImmutableList.of(resource1, resource2).iterator();
        underTest = context.registerInjectActivateService(new ConfigurationInheritanceStrategyMultiplexerImpl());
    }

    @Test
    public void testWithNoStrategies() {
        assertNull(underTest.getResource(resources));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testWithOneStrategy() {
        ConfigurationInheritanceStrategy strategy = mock(ConfigurationInheritanceStrategy.class);
        
        when(strategy.getResource((Iterator<Resource>)any())).thenAnswer(new Answer<Resource>() {
            @Override
            public Resource answer(InvocationOnMock invocation) throws Throwable {
                Iterator<Resource> items = invocation.getArgument(0);
                return items.next();
            }
        });
        
        context.registerService(ConfigurationInheritanceStrategy.class, strategy);
        
        assertSame(resource1, underTest.getResource(resources));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWithMultipleStrategies() {
        ConfigurationInheritanceStrategy strategy1 = mock(ConfigurationInheritanceStrategy.class);
        ConfigurationInheritanceStrategy strategy2 = mock(ConfigurationInheritanceStrategy.class);
        ConfigurationInheritanceStrategy strategy3 = mock(ConfigurationInheritanceStrategy.class);
        
        when(strategy1.getResource((Iterator<Resource>)any())).thenAnswer(new Answer<Resource>() {
            @Override
            public Resource answer(InvocationOnMock invocation) throws Throwable {
                Iterator<Resource> items = invocation.getArgument(0);
                while (items.hasNext()) {
                    items.next();
                }
                return null;
            }
        });
        when(strategy2.getResource((Iterator<Resource>)any())).thenAnswer(new Answer<Resource>() {
            @Override
            public Resource answer(InvocationOnMock invocation) throws Throwable {
                Iterator<Resource> items = invocation.getArgument(0);
                return items.next();
            }
        });
        
        context.registerService(ConfigurationInheritanceStrategy.class, strategy1);
        context.registerService(ConfigurationInheritanceStrategy.class, strategy2);
        context.registerService(ConfigurationInheritanceStrategy.class, strategy3);
        
        assertSame(resource1, underTest.getResource(resources));
        
        verify(strategy1, times(1)).getResource((Iterator<Resource>)any());
        verify(strategy2, times(1)).getResource((Iterator<Resource>)any());
        verifyNoMoreInteractions(strategy3);
    }

}
