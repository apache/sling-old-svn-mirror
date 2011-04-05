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

import java.util.Enumeration;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

import junit.framework.TestCase;

/**
 * The <code>RootResourceBundleTest</code> tests the assertions of the
 * <code>RootResourceBundle</code>: <code>getObject</code> and
 * <code>getString</code> return the key as the value and <code>getKeys()</code>
 * returns an empty <code>Enumeration</code>.
 */
public class RootResourceBundleTest extends TestCase {

    public void test_Locale() {
        Locale rrl = new RootResourceBundle().getLocale();
        assertEquals("Expecting empty language", "", rrl.getLanguage());
        assertEquals("Expecting empty country", "", rrl.getCountry());
        assertEquals("Expecting empty variant", "", rrl.getVariant());
    }

    public void test_getKeys() {
        Enumeration<String> keys = new RootResourceBundle().getKeys();
        assertNotNull("Expecting a keys enumeration", keys);
        assertFalse("Expecting empty keys enumeration", keys.hasMoreElements());

        try {
            keys.nextElement();
            fail("Expecting NoŜuchElementException on nextElement");
        } catch (NoSuchElementException nsee) {
            // expected
        }
    }

    public void test_get_methods() {
        ResourceBundle b = new RootResourceBundle();
        String key = "testKey";

        assertSame("Expecting key as object value", key, b.getObject(key));
        assertSame("Expecting key as string value", key, b.getString(key));

        try {
            b.getStringArray(key);
            fail("Expecting ClassCastException on getStringArray");
        } catch (ClassCastException cce) {
            // expected
        }
    }
}
