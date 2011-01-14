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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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

    private InstallableResource getInstallableResource() {
        return new InstallableResource("1",
                null, new Hashtable<String, Object>(), null, null, null);
    }

    @Test public void testConstructor() throws IOException {
        final InternalResource ir = InternalResource.create(SCHEME,
                getInstallableResource());
        assertTrue(ir.getURL().startsWith(SCHEME + ':'));
        assertNotNull(ir.getDictionary());
        assertNotNull(ir.getPrivateCopyOfDictionary());
        assertNull(ir.getInputStream());
        assertNull(ir.getPrivateCopyOfFile());
        assertEquals(InstallableResource.TYPE_PROPERTIES, ir.getType());
    }
}
