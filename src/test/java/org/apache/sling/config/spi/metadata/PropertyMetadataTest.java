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
package org.apache.sling.config.spi.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PropertyMetadataTest {

    @Test
    public void testValid() {
        PropertyMetadata<String> underTest = new PropertyMetadata<String>("testParam", String.class);
        assertEquals("testParam", underTest.getName());
        assertEquals(String.class, underTest.getType());
        assertNull(underTest.getDefaultValue());
        
        underTest.getProperties().put("p1", "v1");
        assertEquals("v1", underTest.getProperties().get("p1", String.class));

        underTest.setDefaultValue("defValue");
        assertEquals("defValue", underTest.getDefaultValue());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidName() {
        new PropertyMetadata<String>("$illegal name", String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidType() {
        new PropertyMetadata<Object>("testParam", Object.class);
    }

}
