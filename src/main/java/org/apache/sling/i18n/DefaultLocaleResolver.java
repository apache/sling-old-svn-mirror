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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * The <code>DefaultLocaleResolver</code> resolves the request's Locale by
 * calling the <code>ServletRequest.getLocales()</code> method, which generally
 * will be the Servlet Container's implementation of this method and thus be
 * based on the client's <code>Accept-Language</code> header.
 */
public class DefaultLocaleResolver implements LocaleResolver, RequestLocaleResolver {

    /**
     * Return the Locales provided by the
     * <code>ServletRequest.getLocales()</code> method collected in a
     * <code>List</code>.
     */
    public List<Locale> resolveLocale(final SlingHttpServletRequest request) {
        return this.resolveLocale((HttpServletRequest)request);
    }

    /**
     * @see org.apache.sling.i18n.RequestLocaleResolver#resolveLocale(javax.servlet.http.HttpServletRequest)
     */
    public List<Locale> resolveLocale(final HttpServletRequest request) {
        Enumeration<?> locales = request.getLocales();
        ArrayList<Locale> localeList = new ArrayList<Locale>();
        while (locales.hasMoreElements()) {
            localeList.add((Locale) locales.nextElement());
        }
        return localeList;
    }
}
