/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl.filter;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Locale;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.impl.RequestData;
import org.apache.sling.locale.LocaleResolver;


/**
 * The <code>LocaleResolverFilter</code> TODO
 *
 * @scr.component immediate="true" label="%locale.name"
 *      description="%locale.description"
 * @scr.property name="service.description" value="Locale Resolver Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-700" type="Integer" private="true"
 * @scr.service
 */
public class LocaleResolverFilter implements ComponentFilter {

    /**
     * @scr.property value="en_US"
     */
    public static final String PAR_DEFAULT_LOCALE = "locale.default";

    /**
     * @scr.reference target="" cardinality="0..1" policy="dynamic"
     */
    private LocaleResolver localeResolver;

    private Locale defaultLocale;

    /**
     * @see org.apache.sling.component.ComponentFilter#doFilter(org.apache.sling.component.ComponentRequest, org.apache.sling.component.ComponentResponse, org.apache.sling.component.ComponentFilterChain)
     */
    public void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException {

        // assert request data on request (may throw ComponentException if none)
        RequestData requestData = RequestData.getRequestData(request);

        // get locale from Servlet request or locale resolver
        Locale requestLocale;
        if (this.localeResolver == null) {
            requestLocale = requestData.getServletRequest().getLocale();
        } else {
            requestLocale = this.localeResolver.resolveLocale(request);
        }

        // assert at least default locale
        if (requestLocale == null) {
            requestLocale = this.defaultLocale;
        }

        requestData.setLocale(requestLocale);

        // continue request processing without any more intervention
        filterChain.doFilter(request, response);
    }

    public void init(ComponentContext context) {
    }

    public void destroy() {
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(org.osgi.service.component.ComponentContext context) {
        Dictionary configuration = context.getProperties();
        String localeString = (String) configuration.get(PAR_DEFAULT_LOCALE);
        this.defaultLocale = this.toLocale(localeString);
    }

    protected void bindLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    protected void unbindLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = null;
    }

    // ---------- internal -----------------------------------------------------

    private Locale toLocale(String localeString) {
        if (localeString == null || localeString.length() == 0) {
            return Locale.getDefault();
        }

        // check whether we have an exact match locale
        Locale[] available = Locale.getAvailableLocales();
        for (int i = 0; i < available.length; i++) {
            if (available[i].toString().equals(localeString)) {
                return available[i];
            }
        }

        // check language and country
        String[] parts = localeString.split("_");
        if (parts.length == 0) {
            return Locale.getDefault();
        }

        // at least language is available
        String lang = parts[0];
        String[] langs = Locale.getISOLanguages();
        for (int i = 0; i < langs.length; i++) {
            if (langs[i].equals(lang)) {
                lang = null; // signal ok
                break;
            }
        }
        if (lang != null) {
            parts[0] = Locale.getDefault().getLanguage();
        }

        // only language
        if (parts.length == 1) {
            return new Locale(parts[0]);
        }

        // country is also available
        String country = parts[1];
        String[] countries = Locale.getISOCountries();
        for (int i = 0; i < countries.length; i++) {
            if (countries[i].equals(lang)) {
                lang = null; // signal ok
                break;
            }
        }
        if (country != null) {
            parts[1] = Locale.getDefault().getCountry();
        }

        // language and country
        if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        }

        // language, country and variant
        return new Locale(parts[0], parts[1], parts[2]);
    }
}
