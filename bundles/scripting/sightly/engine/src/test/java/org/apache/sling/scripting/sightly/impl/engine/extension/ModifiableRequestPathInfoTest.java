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
package org.apache.sling.scripting.sightly.impl.engine.extension;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ModifiableRequestPathInfoTest {

    private static final String EMPTY = "";
    private static final String PATH = "test";
    private static final String SELECTOR_STRING = "a.b";
    private static final String[] SELECTOR_STRING_SET = new String[]{"a", "b"};
    private static final String EXTENSION = "html";
    private static final String SUFFIX = "/suffix1/suffix2";

    private static final URIManipulationFilterExtension.ModifiableRequestPathInfo emptyPathInfo =
            new URIManipulationFilterExtension.ModifiableRequestPathInfo(EMPTY);
    private static final URIManipulationFilterExtension.ModifiableRequestPathInfo simplePath =
            new URIManipulationFilterExtension.ModifiableRequestPathInfo(PATH);
    private static final URIManipulationFilterExtension.ModifiableRequestPathInfo
            pathWithExtension = new URIManipulationFilterExtension.ModifiableRequestPathInfo(PATH + "." + EXTENSION);
    private static final URIManipulationFilterExtension.ModifiableRequestPathInfo
            pathWithSelectors =
            new URIManipulationFilterExtension.ModifiableRequestPathInfo(PATH + "." + SELECTOR_STRING + "." + EXTENSION);
    private static final URIManipulationFilterExtension.ModifiableRequestPathInfo
            pathWithSelectorsSuffix =
            new URIManipulationFilterExtension.ModifiableRequestPathInfo(PATH + "." + SELECTOR_STRING + "." + EXTENSION + SUFFIX);
    private static final URIManipulationFilterExtension.ModifiableRequestPathInfo
            pathWithMultipleDotsSuffix = new URIManipulationFilterExtension.ModifiableRequestPathInfo(
            "test/child.name/resource." + SELECTOR_STRING + "." + EXTENSION + SUFFIX);


    @Test
    public void testGetResourcePath() throws Exception {
        assertEquals(EMPTY, emptyPathInfo.getResourcePath());
        assertEquals(PATH, simplePath.getResourcePath());
        assertEquals(PATH, pathWithExtension.getResourcePath());
        assertEquals(PATH, pathWithSelectors.getResourcePath());
        assertEquals(PATH, pathWithSelectorsSuffix.getResourcePath());
        assertEquals("test/child", pathWithMultipleDotsSuffix.getResourcePath());
    }

    @Test
    public void testToString() throws Exception {
        assertEquals(EMPTY, emptyPathInfo.toString());
        assertEquals(PATH, simplePath.toString());
        assertEquals(PATH + "." + EXTENSION, pathWithExtension.toString());
        assertEquals(PATH + "." + SELECTOR_STRING + "." + EXTENSION, pathWithSelectors.toString());
        assertEquals(PATH + "." + SELECTOR_STRING + "." + EXTENSION + SUFFIX, pathWithSelectorsSuffix.toString());
        assertEquals("test/child.name/resource." + SELECTOR_STRING + "." + EXTENSION + SUFFIX, pathWithMultipleDotsSuffix.toString());
    }

    @Test
    public void testGetSelectors() throws Exception {
        assertEquals(0, emptyPathInfo.getSelectors().length);
        assertEquals(0, simplePath.getSelectors().length);
        assertEquals(0, pathWithExtension.getSelectors().length);
        assertArrayEquals(SELECTOR_STRING_SET, pathWithSelectors.getSelectors());
        assertArrayEquals(SELECTOR_STRING_SET, pathWithSelectorsSuffix.getSelectors());
        assertEquals(0, pathWithMultipleDotsSuffix.getSelectors().length);
    }

    @Test
    public void testGetExtension() throws Exception {
        assertNull(emptyPathInfo.getExtension());
        assertNull(simplePath.getExtension());
        assertEquals(EXTENSION, pathWithExtension.getExtension());
        assertEquals(EXTENSION, pathWithSelectors.getExtension());
        assertEquals(EXTENSION, pathWithSelectorsSuffix.getExtension());
        assertEquals("name", pathWithMultipleDotsSuffix.getExtension());
    }

    @Test
    public void testGetSuffix() {
        assertNull(emptyPathInfo.getSuffix());
        assertNull(simplePath.getSuffix());
        assertNull(pathWithExtension.getSuffix());
        assertNull(pathWithSelectors.getSuffix());
        assertEquals(SUFFIX, pathWithSelectorsSuffix.getSuffix());
        assertEquals("/resource." + SELECTOR_STRING + "." + EXTENSION + SUFFIX, pathWithMultipleDotsSuffix.getSuffix());
    }

}
