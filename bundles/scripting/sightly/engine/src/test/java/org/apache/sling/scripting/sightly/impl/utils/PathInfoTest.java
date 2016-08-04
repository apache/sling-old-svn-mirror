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
package org.apache.sling.scripting.sightly.impl.utils;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.sling.scripting.sightly.impl.engine.extension.URIManipulationFilterExtension;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PathInfoTest {

    private static final String EMPTY = "";
    private static final String PATH = "test";
    private static final String ABSOLUTE_PATH = "/" + PATH;
    private static final String SCHEME = "http";
    private static final String SCHEME_PATH_SEPARATOR = "//";
    private static final int PORT = 8080;
    private static final String HOST = "www.example.com";
    private static final String SELECTOR_STRING = "a.b";
    private static final Set<String> SELECTOR_STRING_SET = new LinkedHashSet<String>() {{
        add("a");
        add("b");
    }};
    private static final String EXTENSION = "html";
    private static final String SUFFIX = "/suffix1/suffix2";
    private static final String FRAGMENT = "fragment";

    private static final URIManipulationFilterExtension.PathInfo emptyPathInfo = new URIManipulationFilterExtension.PathInfo(EMPTY);
    private static final URIManipulationFilterExtension.PathInfo simplePath = new URIManipulationFilterExtension.PathInfo(PATH);
    private static final URIManipulationFilterExtension.PathInfo
            pathWithExtension = new URIManipulationFilterExtension.PathInfo(PATH + "." + EXTENSION);
    private static final URIManipulationFilterExtension.PathInfo
            pathWithSelectors = new URIManipulationFilterExtension.PathInfo(PATH + "." + SELECTOR_STRING + "." + EXTENSION);
    private static final URIManipulationFilterExtension.PathInfo
            pathWithSelectorsSuffix = new URIManipulationFilterExtension.PathInfo(PATH + "." + SELECTOR_STRING + "." + EXTENSION + SUFFIX);
    private static final URIManipulationFilterExtension.PathInfo pathWithScheme = new URIManipulationFilterExtension.PathInfo(SCHEME + ":" + "//" + HOST);
    private static final URIManipulationFilterExtension.PathInfo
            pathWithSchemePath = new URIManipulationFilterExtension.PathInfo(SCHEME + ":" + "//" + HOST + ABSOLUTE_PATH);
    private static final URIManipulationFilterExtension.PathInfo
            pathWithSchemePathExtension = new URIManipulationFilterExtension.PathInfo(SCHEME + ":" + "//" + HOST + ABSOLUTE_PATH + "." + EXTENSION);
    private static final URIManipulationFilterExtension.PathInfo
            pathWithSchemePathExtensionSelectors = new URIManipulationFilterExtension.PathInfo(SCHEME + ":" + "//" + HOST + ABSOLUTE_PATH + "." +
            SELECTOR_STRING + "." + EXTENSION);
    private static final URIManipulationFilterExtension.PathInfo
            pathWithSchemePathExtensionSelectorsSuffix = new URIManipulationFilterExtension.PathInfo(SCHEME + ":" + "//" + HOST + ABSOLUTE_PATH +
            "." + SELECTOR_STRING + "." + EXTENSION + SUFFIX);
    private static final URIManipulationFilterExtension.PathInfo
            pathWithSchemePathExtensionSelectorsSuffixFragment = new URIManipulationFilterExtension.PathInfo(SCHEME + ":" + "//" + HOST +
            ABSOLUTE_PATH + "." + SELECTOR_STRING + "." + EXTENSION + SUFFIX + "#" + FRAGMENT);
    private static final URIManipulationFilterExtension.PathInfo
            pathWithSchemePortPathExtensionSelectorsSuffixFragment = new URIManipulationFilterExtension.PathInfo(SCHEME + ":" + "//" + HOST +
            ":" + PORT + ABSOLUTE_PATH + "." + SELECTOR_STRING + "." + EXTENSION + SUFFIX + "#" + FRAGMENT);

    @Test
    public void testGetPath() throws Exception {
        assertEquals(EMPTY, emptyPathInfo.getPath());
        assertEquals(PATH, simplePath.getPath());
        assertEquals(PATH, pathWithExtension.getPath());
        assertEquals(PATH, pathWithSelectors.getPath());
        assertEquals(PATH, pathWithSelectorsSuffix.getPath());
        assertEquals(EMPTY, pathWithScheme.getPath());
        assertEquals(ABSOLUTE_PATH, pathWithSchemePath.getPath());
        assertEquals(ABSOLUTE_PATH, pathWithSchemePathExtension.getPath());
        assertEquals(ABSOLUTE_PATH, pathWithSchemePathExtensionSelectors.getPath());
        assertEquals(ABSOLUTE_PATH, pathWithSchemePathExtensionSelectorsSuffix.getPath());
        assertEquals(ABSOLUTE_PATH, pathWithSchemePathExtensionSelectorsSuffixFragment.getPath());
        assertEquals(ABSOLUTE_PATH, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getPath());
    }

    @Test
    public void testGetSelectors() throws Exception {
        assertEquals(0, emptyPathInfo.getSelectors().size());
        assertEquals(0, simplePath.getSelectors().size());
        assertEquals(0, pathWithExtension.getSelectors().size());
        assertEquals(SELECTOR_STRING_SET, pathWithSelectors.getSelectors());
        assertEquals(SELECTOR_STRING_SET, pathWithSelectorsSuffix.getSelectors());
        assertEquals(EMPTY, pathWithScheme.getPath());
        assertEquals(0, pathWithSchemePath.getSelectors().size());
        assertEquals(0, pathWithSchemePathExtension.getSelectors().size());
        assertEquals(SELECTOR_STRING_SET, pathWithSchemePathExtensionSelectors.getSelectors());
        assertEquals(SELECTOR_STRING_SET, pathWithSchemePathExtensionSelectorsSuffix.getSelectors());
        assertEquals(SELECTOR_STRING_SET, pathWithSchemePathExtensionSelectorsSuffixFragment.getSelectors());
        assertEquals(SELECTOR_STRING_SET, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getSelectors());
    }

    @Test
    public void testGetSelectorString() throws Exception {
        assertNull(emptyPathInfo.getSelectorString());
        assertNull(simplePath.getSelectorString());
        assertNull(pathWithExtension.getSelectorString());
        assertEquals(SELECTOR_STRING, pathWithSelectors.getSelectorString());
        assertEquals(SELECTOR_STRING, pathWithSelectorsSuffix.getSelectorString());
        assertNull(pathWithScheme.getSelectorString());
        assertNull(pathWithSchemePath.getSelectorString());
        assertNull(pathWithSchemePathExtension.getSelectorString());
        assertEquals(SELECTOR_STRING, pathWithSchemePathExtensionSelectors.getSelectorString());
        assertEquals(SELECTOR_STRING, pathWithSchemePathExtensionSelectorsSuffix.getSelectorString());
        assertEquals(SELECTOR_STRING, pathWithSchemePathExtensionSelectorsSuffixFragment.getSelectorString());
        assertEquals(SELECTOR_STRING, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getSelectorString());
    }

    @Test
    public void testGetExtension() throws Exception {
        assertNull(emptyPathInfo.getExtension());
        assertNull(simplePath.getExtension());
        assertEquals(EXTENSION, pathWithExtension.getExtension());
        assertEquals(EXTENSION, pathWithSelectors.getExtension());
        assertEquals(EXTENSION, pathWithSelectorsSuffix.getExtension());
        assertNull(pathWithScheme.getExtension());
        assertNull(pathWithSchemePath.getExtension());
        assertEquals(EXTENSION, pathWithSchemePathExtension.getExtension());
        assertEquals(EXTENSION, pathWithSchemePathExtensionSelectors.getExtension());
        assertEquals(EXTENSION, pathWithSchemePathExtensionSelectorsSuffix.getExtension());
        assertEquals(EXTENSION, pathWithSchemePathExtensionSelectorsSuffixFragment.getExtension());
        assertEquals(EXTENSION, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getExtension());
    }

    @Test
    public void testGetScheme() {
        assertNull(emptyPathInfo.getScheme());
        assertNull(simplePath.getScheme());
        assertNull(pathWithExtension.getScheme());
        assertNull(pathWithSelectors.getScheme());
        assertNull(pathWithSelectorsSuffix.getScheme());
        assertEquals(SCHEME, pathWithScheme.getScheme());
        assertEquals(SCHEME, pathWithSchemePath.getScheme());
        assertEquals(SCHEME, pathWithSchemePathExtension.getScheme());
        assertEquals(SCHEME, pathWithSchemePathExtensionSelectors.getScheme());
        assertEquals(SCHEME, pathWithSchemePathExtensionSelectorsSuffix.getScheme());
        assertEquals(SCHEME, pathWithSchemePathExtensionSelectorsSuffixFragment.getScheme());
        assertEquals(SCHEME, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getScheme());
    }

    @Test
    public void testGetBeginPathSeparator() {
        assertNull(emptyPathInfo.getBeginPathSeparator());
        assertNull(simplePath.getBeginPathSeparator());
        assertNull(pathWithExtension.getBeginPathSeparator());
        assertNull(pathWithSelectors.getBeginPathSeparator());
        assertNull(pathWithSelectorsSuffix.getBeginPathSeparator());
        assertEquals(SCHEME_PATH_SEPARATOR, pathWithScheme.getBeginPathSeparator());
        assertEquals(SCHEME_PATH_SEPARATOR, pathWithSchemePath.getBeginPathSeparator());
        assertEquals(SCHEME_PATH_SEPARATOR, pathWithSchemePathExtension.getBeginPathSeparator());
        assertEquals(SCHEME_PATH_SEPARATOR, pathWithSchemePathExtensionSelectors.getBeginPathSeparator());
        assertEquals(SCHEME_PATH_SEPARATOR, pathWithSchemePathExtensionSelectorsSuffix.getBeginPathSeparator());
        assertEquals(SCHEME_PATH_SEPARATOR, pathWithSchemePathExtensionSelectorsSuffixFragment.getBeginPathSeparator());
        assertEquals(SCHEME_PATH_SEPARATOR, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getBeginPathSeparator());
    }

    @Test
    public void testGetHost() {
        assertNull(emptyPathInfo.getHost());
        assertNull(simplePath.getHost());
        assertNull(pathWithExtension.getHost());
        assertNull(pathWithSelectors.getHost());
        assertNull(pathWithSelectorsSuffix.getHost());
        assertEquals(HOST, pathWithScheme.getHost());
        assertEquals(HOST, pathWithSchemePath.getHost());
        assertEquals(HOST, pathWithSchemePathExtension.getHost());
        assertEquals(HOST, pathWithSchemePathExtensionSelectors.getHost());
        assertEquals(HOST, pathWithSchemePathExtensionSelectorsSuffix.getHost());
        assertEquals(HOST, pathWithSchemePathExtensionSelectorsSuffixFragment.getHost());
        assertEquals(HOST, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getHost());
    }

    @Test
    public void testGetPort() {
        assertEquals(-1, emptyPathInfo.getPort());
        assertEquals(-1, simplePath.getPort());
        assertEquals(-1, pathWithExtension.getPort());
        assertEquals(-1, pathWithSelectors.getPort());
        assertEquals(-1, pathWithSelectorsSuffix.getPort());
        assertEquals(-1, pathWithScheme.getPort());
        assertEquals(-1, pathWithSchemePath.getPort());
        assertEquals(-1, pathWithSchemePathExtension.getPort());
        assertEquals(-1, pathWithSchemePathExtensionSelectors.getPort());
        assertEquals(-1, pathWithSchemePathExtensionSelectorsSuffix.getPort());
        assertEquals(-1, pathWithSchemePathExtensionSelectorsSuffixFragment.getPort());
        assertEquals(PORT, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getPort());
    }

    @Test
    public void testGetSuffix() {
        assertNull(emptyPathInfo.getSuffix());
        assertNull(simplePath.getSuffix());
        assertNull(pathWithExtension.getSuffix());
        assertNull(pathWithSelectors.getSuffix());
        assertEquals(SUFFIX, pathWithSelectorsSuffix.getSuffix());
        assertNull(pathWithScheme.getSuffix());
        assertNull(pathWithSchemePath.getSuffix());
        assertNull(pathWithSchemePathExtension.getSuffix());
        assertNull(pathWithSchemePathExtensionSelectors.getSuffix());
        assertEquals(SUFFIX, pathWithSchemePathExtensionSelectorsSuffix.getSuffix());
        assertEquals(SUFFIX, pathWithSchemePathExtensionSelectorsSuffixFragment.getSuffix());
        assertEquals(SUFFIX, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getSuffix());
    }

    @Test
    public void testGetFragment() {
        assertNull(emptyPathInfo.getFragment());
        assertNull(simplePath.getFragment());
        assertNull(pathWithExtension.getFragment());
        assertNull(pathWithSelectors.getFragment());
        assertNull(pathWithSelectorsSuffix.getFragment());
        assertNull(pathWithScheme.getFragment());
        assertNull(pathWithSchemePath.getFragment());
        assertNull(pathWithSchemePathExtension.getFragment());
        assertNull(pathWithSchemePathExtensionSelectors.getFragment());
        assertNull(pathWithSchemePathExtensionSelectorsSuffix.getFragment());
        assertEquals(FRAGMENT, pathWithSchemePathExtensionSelectorsSuffixFragment.getFragment());
        assertEquals(FRAGMENT, pathWithSchemePortPathExtensionSelectorsSuffixFragment.getFragment());
    }
}
