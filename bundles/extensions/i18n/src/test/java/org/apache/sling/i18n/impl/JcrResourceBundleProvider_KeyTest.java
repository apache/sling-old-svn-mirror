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
package org.apache.sling.i18n.impl;

import java.lang.reflect.Constructor;
import java.util.Locale;

import junit.framework.TestCase;

public class JcrResourceBundleProvider_KeyTest extends TestCase {

    private Constructor<?> ctor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        try {
            Class<?> clazz = getClass().getClassLoader().loadClass(
                "org.apache.sling.i18n.impl.JcrResourceBundleProvider$Key");
            this.ctor = clazz.getDeclaredConstructor(String.class, Locale.class);
        } catch (Throwable t) {
            fail("Cannot get JcrResourceBundleProvider.Key constructor: " + t);
        }
    }

    public void test_key_null_null() {
        final Object key1 = createKey(null, null);
        assertNotNull(key1);
        assertEquals(0, key1.hashCode());

        final Object key2 = createKey(null, null);
        assertNotNull(key2);
        assertEquals(0, key2.hashCode());

        assertEquals(key1, key1);
        assertEquals(key2, key2);
        assertEquals(key1, key2);

        assertFalse(key1.equals(null));
        assertFalse(key2.equals(null));

        assertFalse(key1.equals("string"));
        assertFalse(key2.equals("string"));
    }

    public void test_key_null_Locale() {
        final Object key1 = createKey(null, new Locale("de"));
        assertNotNull(key1);

        final Object key2 = createKey(null, new Locale("de"));
        assertNotNull(key2);

        assertEquals(key1.hashCode(), key2.hashCode());

        assertEquals(key1, key1);
        assertEquals(key2, key2);
        assertEquals(key1, key2);

        assertFalse(key1.equals(null));
        assertFalse(key2.equals(null));

        assertFalse(key1.equals("string"));
        assertFalse(key2.equals("string"));
    }


    public void test_key_String_null() {
        final Object key1 = createKey("base", null);
        assertNotNull(key1);

        final Object key2 = createKey("base", null);
        assertNotNull(key2);

        assertEquals(key1.hashCode(), key2.hashCode());

        assertEquals(key1, key1);
        assertEquals(key2, key2);
        assertEquals(key1, key2);

        assertFalse(key1.equals(null));
        assertFalse(key2.equals(null));

        assertFalse(key1.equals("string"));
        assertFalse(key2.equals("string"));

        assertEquals(key1, createKey("base", null));
        assertFalse(key1.equals(createKey("other", null)));
        assertFalse(key1.equals(createKey("base", new Locale("de"))));
        assertFalse(key1.equals(createKey(null, new Locale("de"))));
        assertFalse(key1.equals(createKey(null, null)));
    }


    public void test_key_String_Locale() {
        final Object key1 = createKey("base", new Locale("de"));
        assertNotNull(key1);

        final Object key2 = createKey("base", new Locale("de"));
        assertNotNull(key2);

        assertEquals(key1.hashCode(), key2.hashCode());

        assertEquals(key1, key1);
        assertEquals(key2, key2);
        assertEquals(key1, key2);

        assertFalse(key1.equals(null));
        assertFalse(key2.equals(null));

        assertFalse(key1.equals("string"));
        assertFalse(key2.equals("string"));

        assertEquals(key1, createKey("base", new Locale("de")));
        assertFalse(key1.equals(createKey("other", null)));
        assertFalse(key1.equals(createKey("other", new Locale("de"))));
        assertFalse(key1.equals(createKey("base", new Locale("en"))));
        assertFalse(key1.equals(createKey(null, new Locale("de"))));
        assertFalse(key1.equals(createKey(null, null)));
    }

    private Object createKey(final String baseName, final Locale locale) {
        try {
            return ctor.newInstance(baseName, locale);
        } catch (Throwable t) {
            fail("Cannot create Key instance: " + t);
            return null; // keep compiler quiet
        }
    }
}
