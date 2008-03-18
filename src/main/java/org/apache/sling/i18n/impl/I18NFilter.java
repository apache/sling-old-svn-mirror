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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.i18n.LocaleResolver;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>I18NFilter</code> class is a request level filter, which provides
 * the resource bundle for the current request.
 * 
 * @scr.component immediate="true" metatype="false"
 * @scr.property name="service.description" value="Internationalization Support
 *               Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-700" type="Integer" private="true"
 * @scr.service
 */
public class I18NFilter implements Filter {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(I18NFilter.class.getName());

    /**
     * @scr.property value="en_US"
     */
    public static final String PAR_DEFAULT_LOCALE = "locale.default";

    private static LocaleResolver DEFAULT_LOCALE_RESOLVER = new LocaleResolver() {
        public List<Locale> resolveLocale(SlingHttpServletRequest request) {

            // unwrap the request which is a I18NSlingHttpServletRequest
            request = ((SlingHttpServletRequestWrapper) request).getSlingRequest();

            Enumeration<?> locales = request.getLocales();
            List<Locale> localeList = new ArrayList<Locale>();
            while (locales.hasMoreElements()) {
                localeList.add((Locale) locales.nextElement());
            }

            return localeList;
        }
    };

    /**
     * @scr.reference cardinality="0..1" policy="dynamic"
     */
    private LocaleResolver localeResolver = DEFAULT_LOCALE_RESOLVER;

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    ResourceBundleProvider resourceBundleProvider;

    private Locale defaultLocale;

    public void init(FilterConfig filterConfig) {
        // nothing to do
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        // wrap with our ResourceBundle provisioning
        request = new I18NSlingHttpServletRequest(request,
            resourceBundleProvider, localeResolver, defaultLocale);

        // and forward the request
        chain.doFilter(request, response);
    }

    public void destroy() {
        // nothing to do
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(org.osgi.service.component.ComponentContext context) {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> configuration = context.getProperties();
        String localeString = (String) configuration.get(PAR_DEFAULT_LOCALE);
        this.defaultLocale = this.toLocale(localeString);
    }

    protected void bindLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    protected void unbindLocaleResolver(LocaleResolver localeResolver) {
        if (this.localeResolver == localeResolver) {
            this.localeResolver = DEFAULT_LOCALE_RESOLVER;
        }
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

    // ---------- internal class -----------------------------------------------

    private static class I18NSlingHttpServletRequest extends
            SlingHttpServletRequestWrapper {

        private final ResourceBundleProvider bundleProvider;

        private final LocaleResolver localeResolver;

        private final Locale defaultLocale;

        private Locale locale;

        private List<Locale> localeList;

        I18NSlingHttpServletRequest(ServletRequest delegatee,
                ResourceBundleProvider bundleProvider,
                LocaleResolver localeResolver, Locale defaultLocale) {
            super((SlingHttpServletRequest) delegatee);
            this.bundleProvider = bundleProvider;
            this.localeResolver = localeResolver;
            this.defaultLocale = defaultLocale;
        }

        @Override
        public ResourceBundle getResourceBundle(Locale locale) {
            if (bundleProvider != null) {
                if (locale == null) {
                    locale = getLocale();
                }

                try {
                    return bundleProvider.getResourceBundle(locale);
                } catch (MissingResourceException mre) {
                    log.warn(
                        "getResourceBundle: Cannot get ResourceBundle from provider",
                        mre);
                }
            } else {
                log.info("getResourceBundle: ResourceBundleProvider not available, calling default implementation");
            }

            return super.getResourceBundle(locale);
        }

        @Override
        public Locale getLocale() {
            if (locale == null) {
                List<Locale> localeList = getLocaleList();
                if (localeList.isEmpty()) {
                    locale = defaultLocale;
                } else {
                    locale = localeList.get(0);
                }
            }

            return locale;
        }

        @Override
        public Enumeration<?> getLocales() {
            return Collections.enumeration(getLocaleList());
        }

        private List<Locale> getLocaleList() {
            if (localeList == null) {
                localeList = localeResolver.resolveLocale(this);
            }

            return localeList;
        }
    }

}
