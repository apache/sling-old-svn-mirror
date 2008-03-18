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
package org.apache.sling.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The <code>ResourceBundleProvider</code> service interface defines the API
 * for a service, which is capable of returned <code>ResourceBundle</code> for
 * given any <code>Locale</code>.
 */
public interface ResourceBundleProvider {

    /**
     * Returns a <code>ResourceBundle</code> for the given locale.
     * 
     * @param locale The <code>Locale</code> for which to return the resource
     *            bundle. If this is <code>null</code> the platform default
     *            locale as returned by <code>Locale.getDefault()</code> is
     *            assumed.
     * @return The <code>ResourceBundle</code> for the given locale
     * @throws MissingResourceException If the service is not capable of
     *             returning a <code>ResourceBundle</code>
     */
    ResourceBundle getResourceBundle(Locale locale);

}
