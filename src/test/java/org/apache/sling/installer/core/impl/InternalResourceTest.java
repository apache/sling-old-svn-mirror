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
package org.apache.sling.installer.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.core.impl.mocks.MockFileDataStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
/**
 * Test for the installable resource.
 */
public class InternalResourceTest {

    private static final String SCHEME = "test";

    @Before public void setDataStore() {
        MockFileDataStore.set();
    }

    @After public void unsetDataStore() {
        MockFileDataStore.unset();
    }

    private Dictionary<String, Object> getSimpleDict() {
        final Hashtable<String, Object> dict = new Hashtable<String, Object>();
        dict.put("a", "a");
        dict.put("b", 2);

        return dict;
    }

    private void assertIsSimpleDict(final Dictionary<String, Object> dict) {
        assertEquals(2, dict.size());
        assertEquals("a", dict.get("a"));
        assertEquals(2, dict.get("b"));
    }

    @Test public void testSimpleProps() throws IOException {
        final String[] types = new String[] {InstallableResource.TYPE_CONFIG,
                InstallableResource.TYPE_PROPERTIES,
                null, "zip"};

        for(int i=0;i<types.length; i++) {
            final InstallableResource instRes = new InstallableResource("1",
                    null, getSimpleDict(), null, types[i], null);

            final InternalResource ir = InternalResource.create(SCHEME,
                    instRes);
            assertEquals(SCHEME + ":1", ir.getURL());
            assertEquals("1", ir.getId());
            assertNotNull(ir.getDictionary());
            assertIsSimpleDict(ir.getDictionary());
            assertNotNull(ir.getPrivateCopyOfDictionary());
            assertIsSimpleDict(ir.getPrivateCopyOfDictionary());
            assertNull(ir.getInputStream());
            assertNull(ir.getPrivateCopyOfFile());
            if ( "zip".equals(types[i]) ) {
                assertEquals("zip", ir.getType());
            } else {
                assertEquals(InstallableResource.TYPE_PROPERTIES, ir.getType());
            }
            assertNotNull(ir.getDigest());
            assertEquals(InstallableResource.DEFAULT_PRIORITY, ir.getPriority());
        }
    }
}
