/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.scripting.core.impl.helper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.script.SimpleBindings;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Test of ProtectedBindings.
 *
 */
public class ProtectedBindingsTest {

    private ProtectedBindings bindings;

    @Before
    public void setup() {
        SimpleBindings inner = new SimpleBindings();
        inner.put("test1", "value1");
        this.bindings = new ProtectedBindings(inner, Collections.singleton("test1"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOverwriteDisallowed() {
        bindings.put("test1", "value2");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRemoveDisallowed() {
        bindings.remove("test1");
    }

    @Test
    public void testAddingAllowed() {
        bindings.put("test2", "value2");
    }

    @Test
    public void testOverwriteNonProtectedAllowed() {
        bindings.put("test2", "value2");
        bindings.put("test2", "value3");
    }


    @Test
    public void testPuttingMapOverwritesSelectively() {
        bindings.put("test2", "value2");

        Map<String,Object> toMerge = new HashMap<String, Object>();
        toMerge.put("test1", "value2");
        toMerge.put("test2", "value3");

        bindings.putAll(toMerge);

        assertEquals("value1", bindings.get("test1"));
        assertEquals("value3", bindings.get("test2"));

    }

}
