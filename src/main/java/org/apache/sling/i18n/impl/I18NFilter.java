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
 * @scr.component immediate="true" metatype="no"
 * @scr.property name="service.description" value="Internationalization Support Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-700" type="Integer" private="true"
 * @scr.service
 */
public class I18NFilter implements Filter {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(I18NFilter.class.getName());

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

    public void init(FilterConfig filterConfig) {
        // nothing to do
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        // wrap with our ResourceBundle provisioning
        request = new I18NSlingHttpServletRequest(request,
            resourceBundleProvider, localeResolver);

        // and forward the request
        chain.doFilter(request, response);
    }

    public void destroy() {
        // nothing to do
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void bindLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    protected void unbindLocaleResolver(LocaleResolver localeResolver) {
        if (this.localeResolver == localeResolver) {
            this.localeResolver = DEFAULT_LOCALE_RESOLVER;
        }
    }

    // ---------- internal -----------------------------------------------------

    // ---------- internal class -----------------------------------------------

    private static class I18NSlingHttpServletRequest extends
            SlingHttpServletRequestWrapper {

        private final ResourceBundleProvider bundleProvider;

        private final LocaleResolver localeResolver;

        private Locale locale;

        private List<Locale> localeList;

        I18NSlingHttpServletRequest(ServletRequest delegatee,
                ResourceBundleProvider bundleProvider,
                LocaleResolver localeResolver) {
            super((SlingHttpServletRequest) delegatee);
            this.bundleProvider = bundleProvider;
            this.localeResolver = localeResolver;
        }

        @Override
        public ResourceBundle getResourceBundle(Locale locale) {
            return getResourceBundle(null, locale);
        }

        @Override
        public ResourceBundle getResourceBundle(String baseName, Locale locale) {
            if (bundleProvider != null) {
                if (locale == null) {
                    locale = getLocale();
                }

                try {
                    return bundleProvider.getResourceBundle(baseName, locale);
                } catch (MissingResourceException mre) {
                    log.warn(
                        "getResourceBundle: Cannot get ResourceBundle from provider",
                        mre);
                }
            } else {
                log.info("getResourceBundle: ResourceBundleProvider not available, calling default implementation");
            }

            return super.getResourceBundle(baseName, locale);
        }
        
        @Override
        public Locale getLocale() {
            if (locale == null) {
                List<Locale> localeList = getLocaleList();
                if (localeList.isEmpty()) {
                    locale = bundleProvider.getDefaultLocale();
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
