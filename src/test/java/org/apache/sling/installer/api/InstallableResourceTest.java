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
package org.apache.sling.installer.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Hashtable;

import org.junit.Test;

/**
 * Test for the installable resource.
 */
public class InstallableResourceTest {

    @Test public void testConstructor() {
        // no id
        try {
            new InstallableResource(null, null, new Hashtable<String, Object>(), null, null, null);
            fail("IAE expected : no id");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        // no input stream, no dict
        try {
            new InstallableResource("1", null, null, null, null, null);
            fail("IAE expected : no is no dict");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        new InstallableResource("1", new ByteArrayInputStream("a".getBytes()), null, null, null, null);
        new InstallableResource("1", null, new Hashtable<String, Object>(), null, null, null);
        new InstallableResource("1",  new ByteArrayInputStream("a".getBytes()), new Hashtable<String, Object>(), null, null, null);
    }

    @Test public void testPriority() {
        final InstallableResource defaultPrio =
            new InstallableResource("1", null, new Hashtable<String, Object>(), null, null, null);
        assertEquals(InstallableResource.DEFAULT_PRIORITY, defaultPrio.getPriority());
        final InstallableResource ownPrio =
            new InstallableResource("1", null, new Hashtable<String, Object>(), null, null, 47);
        assertEquals(47, ownPrio.getPriority());
    }
}
