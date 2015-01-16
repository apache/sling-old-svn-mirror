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
package org.apache.sling.models.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.apache.sling.models.spi.ImplementationPicker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AdapterImplementationsTest {

    private static final Class<?> SAMPLE_ADAPTER = Comparable.class;
    private static final Object SAMPLE_ADAPTABLE = new Object();    

    private AdapterImplementations underTest;
    
    @Before
    public void setUp() {
        underTest = new AdapterImplementations();
        underTest.setImplementationPickers(Arrays.asList(new ImplementationPicker[] {
            new FirstImplementationPicker()
        }));
    }
    
    @Test
    public void testNoMapping() {
        assertNull(underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE));
        
        // make sure this raises no exception
        underTest.remove(SAMPLE_ADAPTER.getName(), String.class.getName());
    }
    
    @Test
    public void testSingleMapping() {
        underTest.add(SAMPLE_ADAPTER, String.class);
        
        assertEquals(String.class, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());
        
        underTest.remove(SAMPLE_ADAPTER.getName(), String.class.getName());

        assertNull(underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE));
    }

    @Test
    public void testMultipleMappings() {
        underTest.add(SAMPLE_ADAPTER, String.class);
        underTest.add(SAMPLE_ADAPTER, Integer.class);
        underTest.add(SAMPLE_ADAPTER, Long.class);
        
        assertEquals(Integer.class, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());
        
        underTest.remove(SAMPLE_ADAPTER.getName(), Integer.class.getName());

        assertEquals(Long.class, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());

        underTest.remove(SAMPLE_ADAPTER.getName(), Long.class.getName());
        underTest.remove(SAMPLE_ADAPTER.getName(), String.class.getName());
        
        assertNull(underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE));
    }
    
    @Test
    public void testRemoveAll() {
        underTest.add(SAMPLE_ADAPTER, String.class);
        underTest.add(SAMPLE_ADAPTER, Integer.class);
        underTest.add(SAMPLE_ADAPTER, Long.class);
        
        underTest.removeAll();
        
        assertNull(underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE));
    }
    
    @Test
    public void testMultipleImplementationPickers() {
        underTest.setImplementationPickers(Arrays.asList(
            new NoneImplementationPicker(),
            new LastImplementationPicker(),
            new FirstImplementationPicker()
        ));

        underTest.add(SAMPLE_ADAPTER, String.class);
        underTest.add(SAMPLE_ADAPTER, Integer.class);
        underTest.add(SAMPLE_ADAPTER, Long.class);
        
        assertEquals(String.class, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());
    }
    
    @Test
    public void testSimpleModel() {
        underTest.add(SAMPLE_ADAPTER, SAMPLE_ADAPTER);
        
        assertEquals(SAMPLE_ADAPTER, underTest.lookup(SAMPLE_ADAPTER, SAMPLE_ADAPTABLE).getType());
    }
    
    static final class NoneImplementationPicker implements ImplementationPicker {
        @Override
        public Class<?> pick(Class<?> adapterType, Class<?>[] implementationsTypes, Object adaptable) {
            return null;
        }        
    }
    
    static final class LastImplementationPicker implements ImplementationPicker {
        @Override
        public Class<?> pick(Class<?> adapterType, Class<?>[] implementationsTypes, Object adaptable) {
            return implementationsTypes[implementationsTypes.length - 1];
        }        
    }
    
}
