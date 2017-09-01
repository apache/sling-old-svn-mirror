/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.runtime;

import java.util.HashMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import static org.junit.Assert.*;

public class SlingRuntimeObjectModelTest {

    private static final String VALUE_MAP_VALUE = "ValueMap value";
    private static final String METHOD_VALUE = "Method value";
    private SlingRuntimeObjectModel slingRuntimeObjectModel = new SlingRuntimeObjectModel();

    @Test
    public void getPropertyFromAdaptableWithField() throws Exception {
        assertEquals("Expected public fields to have priority over ValueMap adaptable's properties.", FieldTestMockAdaptable.test,
                slingRuntimeObjectModel.getProperty(new FieldTestMockAdaptable(), "test"));
    }

    @Test
    public void getPropertyFromAdaptableWithMethod() throws Exception {
        assertEquals("Expected public methods to have priority over ValueMap adaptable's properties.", METHOD_VALUE,
                slingRuntimeObjectModel.getProperty(new MethodTestMockAdaptable(), "test"));
    }@Test
    public void getPropertyFromAdaptable() throws Exception {
        assertEquals("Expected to solve property from ValueMap returned by an adaptable.", VALUE_MAP_VALUE,
                slingRuntimeObjectModel.getProperty(new AdaptableTestMock(), "test"));
    }

    private abstract class MockAdaptable implements Adaptable {

        ValueMap getValueMap() {
            return new ValueMapDecorator(new HashMap<String, Object>() {{
                put("test", VALUE_MAP_VALUE);
            }});
        }

        @CheckForNull
        @Override
        public <AdapterType> AdapterType adaptTo(@Nonnull Class<AdapterType> aClass) {
            if (aClass == ValueMap.class) {
                return (AdapterType) getValueMap();
            }
            return null;
        }
    }

    public class FieldTestMockAdaptable extends MockAdaptable {

        public static final String test = "Field value";

    }

    public class MethodTestMockAdaptable extends MockAdaptable {
        public String getTest() {
            return METHOD_VALUE;
        }
    }

    public class AdaptableTestMock extends MockAdaptable {}

}
