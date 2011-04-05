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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.i18n.LocaleResolver;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>I18NFilter</code> class is a request level filter, which provides
 * the resource bundle for the current request.
 */
@SlingFilter(generateComponent = false, generateService = true, order = -700, scope = SlingFilterScope.REQUEST)
@Component(immediate = true, metatype = false)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Internationalization Support Filter"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation") })
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

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private LocaleResolver localeResolver = DEFAULT_LOCALE_RESOLVER;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private ResourceBundleProvider resourceBundleProvider;

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
