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

/**
 * The <code>RootResourceBundle</code> is an extremely simple resource bundle
 * which is used as the root resource bundle for the resource bundle hierarchies
 * provided by the {@link JcrResourceBundleProvider}. It has the following
 * functionality:
 * <ul>
 * <li>The {@link #getLocale()} returns a pseudo locale with empty values for
 * all fields (language, country, and variant)</li>
 * <li>The {@link #handleGetObject(String)} always returns the provided
 * <code>key</code> as the value</li>
 * <li>The {@link #getKeys()} method always returns an empty enumeration</li>
 * </ul>
 */
public class RootResourceBundle extends ResourceBundle {

    // The empty enumeration returned fomr getKeys()
    private final Enumeration<String> EMPTY = new Enumeration<String>() {

        public boolean hasMoreElements() {
            return false;
        }

        public String nextElement() {
            throw new NoSuchElementException();
        }
    };

    // The pseudo Locale returned by getLocale()
    private final Locale locale = new Locale("");

    /**
     * Returns a <code>Locale</code> with empty language, country, and variant.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Always returns the <code>key</code> parameter as its value.
     */
    @Override
    protected Object handleGetObject(String key) {
        return key;
    }

    /**
     * Always returns an empty enumeration.
     */
    @Override
    public Enumeration<String> getKeys() {
        return EMPTY;
    }

}
