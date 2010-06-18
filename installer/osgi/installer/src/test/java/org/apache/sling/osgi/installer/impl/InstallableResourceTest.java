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
package org.apache.sling.osgi.installer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.InstallableResourceFactory;
import org.junit.Test;

public class InstallableResourceTest {

    private InstallableResourceFactory factory = new InstallableResourceFactoryImpl();

    @Test
    public void testDictionaryDigest() {
        final Dictionary<String, Object> d = new Hashtable<String, Object>();
        final InstallableResource r = factory.create("x:url", d, null, null, null);
        assertNotNull("Expected InstallableResource to compute its own digest", r.getDigest());
    }

    @org.junit.Test public void testDictionaryDigestFromDictionaries() throws Exception {
        final Hashtable<String, Object> d1 = new Hashtable<String, Object>();
        final Hashtable<String, Object> d2 = new Hashtable<String, Object>();

        final String [] keys = { "foo", "bar", "something" };
        for(int i=0 ; i < keys.length; i++) {
            d1.put(keys[i], keys[i] + "." + keys[i]);
        }
        for(int i=keys.length - 1 ; i >= 0; i--) {
            d2.put(keys[i], keys[i] + "." + keys[i]);
        }

        final InstallableResource r1 = factory.create("test:url1", d1, null, null, null);
        final InstallableResource r2 = factory.create("test:url1", d2, null, null, null);

        assertEquals(
                "Two InstallableResource (Dictionary) with same values but different key orderings must have the same key",
                r1.getDigest(),
                r2.getDigest()
        );
    }
}
