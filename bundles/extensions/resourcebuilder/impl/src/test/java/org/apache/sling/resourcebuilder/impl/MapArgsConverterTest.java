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
package org.apache.sling.resourcebuilder.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.junit.Test;

public class MapArgsConverterTest {
    
    @Test
    public void validArguments() throws PersistenceException {
        final Map<String, Object> m = MapArgsConverter.toMap("foo", "bar", "count", 21);
        assertEquals(2, m.size());
        assertEquals("bar", m.get("foo"));
        assertEquals(21, m.get("count"));
    }
    
    @Test
    public void noArguments() throws PersistenceException {
        final Map<String, Object> m = MapArgsConverter.toMap();
        assertTrue(m.isEmpty());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void barArguments() throws PersistenceException {
        MapArgsConverter.toMap("foo", "bar", "count");
    }
}
