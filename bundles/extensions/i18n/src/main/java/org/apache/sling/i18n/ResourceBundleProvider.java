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
 * <p>
 * This interface defines the service API implemented by the existing
 * implementation. It is not intended to be implemented by application bundles.
 * Rather such bundles should get the <code>ResourceBundleProvider</code>
 * service from the service registry to access the data provided.
 */
public interface ResourceBundleProvider {

    /**
     * Request attribute to get the resource bundle.
     * @since 2.2
     */
    String BUNDLE_REQ_ATTR = "org.apache.sling.i18n.resourcebundle";

    /**
     * Returns the default <code>Locale</code> assumed by this instance.
     * @return The default locale
     */
    Locale getDefaultLocale();

    /**
     * Returns a <code>ResourceBundle</code> for the given locale.
     *
     * @param locale The <code>Locale</code> for which to return the resource
     *            bundle. If this is <code>null</code> the default locale as
     *            returned by {@link #getDefaultLocale()} is assumed.
     * @return The <code>ResourceBundle</code> for the given locale
     * @throws java.util.MissingResourceException If the service is not capable of
     *             returning a <code>ResourceBundle</code>
     */
    ResourceBundle getResourceBundle(Locale locale);

    /**
     * Returns a <code>ResourceBundle</code> for the given locale.
     *
     * @param baseName The base name for the resource bundle. If this is
     *            <code>null</code>, the same resource bundle will be
     *            returned as when calling the
     *            {@link #getResourceBundle(Locale)} method.
     * @param locale The <code>Locale</code> for which to return the resource
     *            bundle. If this is <code>null</code> the default locale as
     *            returned by {@link #getDefaultLocale()} is assumed.
     * @return The <code>ResourceBundle</code> for the given locale
     * @throws java.util.MissingResourceException If the service is not capable of
     *             returning a <code>ResourceBundle</code>
     */
    ResourceBundle getResourceBundle(String baseName, Locale locale);

}
