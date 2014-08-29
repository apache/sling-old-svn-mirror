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
package org.apache.sling.models.impl.implpicker;

import static org.junit.Assert.assertEquals;

import org.apache.sling.models.impl.FirstImplementationPicker;
import org.apache.sling.models.spi.ImplementationPicker;
import org.junit.Before;
import org.junit.Test;

public class FirstImplementationPickerTest {

    private static final Class<?> SAMPLE_ADAPTER = Comparable.class;
    private static final Object SAMPLE_ADAPTABLE = new Object();

    private ImplementationPicker underTest;

    @Before
    public void setUp() {
        underTest = new FirstImplementationPicker();
    }

    @Test
    public void testPickOneImplementation() {
        Class<?>[] implementations = new Class<?>[] { String.class };
        assertEquals(String.class, underTest.pick(SAMPLE_ADAPTER, implementations, SAMPLE_ADAPTABLE));
    }

    @Test
    public void testPickMultipleImplementations() {
        Class<?>[] implementations = new Class<?>[] { Integer.class, Long.class, String.class };
        assertEquals(Integer.class, underTest.pick(SAMPLE_ADAPTER, implementations, SAMPLE_ADAPTABLE));
    }

}