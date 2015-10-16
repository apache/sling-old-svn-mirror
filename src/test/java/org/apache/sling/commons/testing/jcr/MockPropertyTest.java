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
package org.apache.sling.commons.testing.jcr;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MockPropertyTest {

    @Test
    public void testIsMultipleFalse() throws Exception {
        MockProperty prop = new MockProperty("prop");
        prop.setValue(new String[] {"val"});

        assertFalse(prop.isMultiple());
        assertFalse(prop.getDefinition().isMultiple());
    }

    @Test
    public void testIsMultipleTrue() throws Exception {
        MockProperty prop = new MockProperty("prop");
        prop.setValue(new String[] {"val1", "val2"});

        assertTrue(prop.isMultiple());
        assertTrue(prop.getDefinition().isMultiple());
    }

}
