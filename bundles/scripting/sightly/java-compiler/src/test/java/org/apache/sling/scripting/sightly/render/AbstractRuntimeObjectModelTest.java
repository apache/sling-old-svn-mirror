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
package org.apache.sling.scripting.sightly.render;

import org.junit.Test;

import static org.junit.Assert.*;

public class AbstractRuntimeObjectModelTest {

    private AbstractRuntimeObjectModel runtimeObjectModel = new AbstractRuntimeObjectModel() {};

    @Test
    public void testResolveProperty_ArrayLength() throws Exception {
        int[] ints = new int[] {1, 2, 3};
        Integer[] integers = new Integer[] {1, 2, 3};
        assertEquals(ints.length, runtimeObjectModel.resolveProperty(ints, "length"));
        assertEquals(integers.length, runtimeObjectModel.resolveProperty(integers, "length"));
    }
}
