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

import java.util.List;
import java.util.Locale;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * The <code>LocaleResolver</code> service interface may be implemented by a
 * service registered under this name to allow the resolution of the request
 * <code>Locale</code> to apply.
 * <p>
 * This interface is intended to be implemented by providers knowing how to
 * resolve one or more <code>Locale</code>s applicable to handle the request.
 * <p>
 * Only a single <code>LocaleResolver</code> service is currently used.
 * @deprecated The {@link RequestLocaleResolver} should be used instead.
 */
@Deprecated
public interface LocaleResolver {

    /**
     * Return a non-<code>null</code> but possiby empty list of
     * <code>Locale</code> instances to consider for localization of the current
     * request. The list returned is assumed to be ordered by preference where
     * the first entry is the prefered <code>Locale</code> and the last entry is
     * the least prefered <code>Locale</code>.
     * <p>
     * Returning an empty list is equivalent to returning a singleton list whose
     * single entry is the {@link ResourceBundleProvider#getDefaultLocale()}.
     *
     * @param request The <code>SlingHttpServletRequest</code> providing hints
     *            and information for the <code>Locale</code> resolution.
     * @return The list of <code>Locale</code>s to use for internationalization
     *         of request processing
     */
    List<Locale> resolveLocale(SlingHttpServletRequest request);

}
