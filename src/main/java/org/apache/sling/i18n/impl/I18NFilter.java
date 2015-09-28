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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.apache.sling.i18n.DefaultLocaleResolver;
import org.apache.sling.i18n.LocaleResolver;
import org.apache.sling.i18n.RequestLocaleResolver;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>I18NFilter</code> class is a request level filter, which provides
 * the resource bundle for the current request.
 */
@SlingFilter(generateService = true,
             order = 700, scope = { SlingFilterScope.REQUEST, SlingFilterScope.ERROR })
@Properties({
    @Property(name = "pattern", value="/.*"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Internationalization Support Filter"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation") })
public class I18NFilter implements Filter {

    /** Logger */
    private final static Logger LOG = LoggerFactory.getLogger(I18NFilter.class.getName());

    private final DefaultLocaleResolver DEFAULT_LOCALE_RESOLVER = new DefaultLocaleResolver();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    private volatile LocaleResolver localeResolver = DEFAULT_LOCALE_RESOLVER;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    private volatile RequestLocaleResolver requestLocaleResolver = DEFAULT_LOCALE_RESOLVER;

    @Reference(name = "resourceBundleProvider",
               referenceInterface = ResourceBundleProvider.class,
               cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, ResourceBundleProvider> providers = new TreeMap<Object, ResourceBundleProvider>();

    private volatile ResourceBundleProvider[] sortedProviders = new ResourceBundleProvider[0];

    private final ResourceBundleProvider combinedProvider = new CombinedBundleProvider();

    /** Count the number init() has been called. */
    private volatile int initCount;

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) {
        synchronized(this) {
            initCount++;
        }
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request,
                         final ServletResponse response,
                         final FilterChain chain)
    throws IOException, ServletException {
        final boolean runGlobal = this.initCount == 2;
        if ( request instanceof SlingHttpServletRequest ) {
            // check if we can use the simple version to wrap
            if ( !runGlobal || this.requestLocaleResolver == DEFAULT_LOCALE_RESOLVER ) {
                // wrap with our ResourceBundle provisioning
                request = new I18NSlingHttpServletRequest(request,
                        combinedProvider, localeResolver);
            } else {
                request = new BaseI18NSlingHttpServletRequest(request, combinedProvider);
            }
        } else {
            request = new I18NHttpServletRequest(request,
                    combinedProvider, requestLocaleResolver);
        }

        // and forward the request
        chain.doFilter(request, response);
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        synchronized(this) {
            initCount--;
        }
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void bindLocaleResolver(final LocaleResolver resolver) {
        this.localeResolver = resolver;
    }

    protected void unbindLocaleResolver(final LocaleResolver resolver) {
        if (this.localeResolver == resolver) {
            this.localeResolver = DEFAULT_LOCALE_RESOLVER;
        }
    }

    protected void bindRequestLocaleResolver(final RequestLocaleResolver resolver) {
        this.requestLocaleResolver = resolver;
    }

    protected void unbindRequestLocaleResolver(final RequestLocaleResolver resolver) {
        if (this.requestLocaleResolver == resolver) {
            this.requestLocaleResolver = DEFAULT_LOCALE_RESOLVER;
        }
    }

    protected void bindResourceBundleProvider(final ResourceBundleProvider provider, final Map<String, Object> props) {
        synchronized ( this.providers ) {
            this.providers.put(ServiceUtil.getComparableForServiceRanking(props), provider);
            this.sortedProviders = this.providers.values().toArray(new ResourceBundleProvider[this.providers.size()]);
        }
    }

    protected void unbindResourceBundleProvider(final ResourceBundleProvider provider, final Map<String, Object> props) {
        synchronized ( this.providers ) {
            this.providers.remove(ServiceUtil.getComparableForServiceRanking(props));
            this.sortedProviders = this.providers.values().toArray(new ResourceBundleProvider[this.providers.size()]);
        }
    }

    // ---------- internal -----------------------------------------------------

    /** Provider that goes through a list of registered providers and takes the first non-null responses */
    private class CombinedBundleProvider implements ResourceBundleProvider {

        @Override
        public Locale getDefaultLocale() {
            // ask all registered providers, use the first one that returns
            final ResourceBundleProvider[] providers = sortedProviders;
            for(int i=providers.length-1; i >= 0; i--) {
                final ResourceBundleProvider provider = providers[i];
                final Locale locale = provider.getDefaultLocale();
                if (locale != null) {
                    return locale;
                }
            }
            return null;
        }

        @Override
        public ResourceBundle getResourceBundle(final Locale locale) {
            // ask all registered providers, use the first one that returns
            final ResourceBundleProvider[] providers = sortedProviders;
            for(int i=providers.length-1; i >= 0; i--) {
                final ResourceBundleProvider provider = providers[i];
                final ResourceBundle bundle = provider.getResourceBundle(locale);
                if (bundle != null) {
                    return bundle;
                }
            }
            return null;
        }

        @Override
        public ResourceBundle getResourceBundle(final String baseName, final Locale locale) {
            // ask all registered providers, use the first one that returns
            final ResourceBundleProvider[] providers = sortedProviders;
            for(int i=providers.length-1; i >= 0; i--) {
                final ResourceBundleProvider provider = providers[i];
                final ResourceBundle bundle = provider.getResourceBundle(baseName, locale);
                if (bundle != null) {
                    return bundle;
                }
            }
            return null;
        }
    }

    // ---------- internal class -----------------------------------------------

    private static class I18NHttpServletRequest
        extends HttpServletRequestWrapper {

        private final ResourceBundleProvider bundleProvider;

        private final RequestLocaleResolver localeResolver;

        private Locale locale;

        private List<Locale> localeList;

        private ResourceBundle resourceBundle;

        I18NHttpServletRequest(final ServletRequest delegatee,
                final ResourceBundleProvider bundleProvider,
                final RequestLocaleResolver localeResolver) {
            super((HttpServletRequest)delegatee);
            this.bundleProvider = bundleProvider;
            this.localeResolver = localeResolver;
        }

        @Override
        public Locale getLocale() {
            if (locale == null) {
                locale = this.getLocaleList().get(0);
            }

            return locale;
        }

        @Override
        public Enumeration<?> getLocales() {
            return Collections.enumeration(getLocaleList());
        }

        @Override
        public Object getAttribute(final String name) {
            if ( ResourceBundleProvider.BUNDLE_REQ_ATTR.equals(name) ) {
                if ( this.resourceBundle == null && this.bundleProvider != null) {
                    this.resourceBundle = this.bundleProvider.getResourceBundle(this.getLocale());
                }
                return this.resourceBundle;
            }
            return super.getAttribute(name);
        }

        private List<Locale> getLocaleList() {
            if (localeList == null) {
                List<Locale> resolved = localeResolver.resolveLocale((HttpServletRequest)this.getRequest());
                this.localeList = (resolved != null && !resolved.isEmpty())
                        ? resolved
                        : Collections.singletonList(this.bundleProvider.getDefaultLocale());
            }

            return localeList;
        }

    }

    private static class BaseI18NSlingHttpServletRequest
        extends SlingHttpServletRequestWrapper {

        protected final ResourceBundleProvider bundleProvider;

        BaseI18NSlingHttpServletRequest(final ServletRequest delegatee,
                final ResourceBundleProvider bundleProvider) {
            super((SlingHttpServletRequest) delegatee);
            this.bundleProvider = bundleProvider;
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
                    LOG.warn(
                        "getResourceBundle: Cannot get ResourceBundle from provider",
                        mre);
                }
            } else {
                LOG.info("getResourceBundle: ResourceBundleProvider not available, calling default implementation");
            }

            return super.getResourceBundle(baseName, locale);
        }
    }

    private static class I18NSlingHttpServletRequest
        extends BaseI18NSlingHttpServletRequest {

        private final LocaleResolver localeResolver;

        private Locale locale;

        private List<Locale> localeList;

        I18NSlingHttpServletRequest(final ServletRequest delegatee,
                final ResourceBundleProvider bundleProvider,
                final LocaleResolver localeResolver) {
            super(delegatee, bundleProvider);
            this.localeResolver = localeResolver;
        }

        @Override
        public Object getAttribute(final String name) {
            if ( ResourceBundleProvider.BUNDLE_REQ_ATTR.equals(name) ) {
                final Object superValue = super.getAttribute(name);
                return (superValue != null ? superValue : this.getResourceBundle(null));
            }
            return super.getAttribute(name);
        }

        @Override
        public Locale getLocale() {
            if (locale == null) {
                locale = this.getLocaleList().get(0);
            }

            return locale;
        }

        @Override
        public Enumeration<?> getLocales() {
            return Collections.enumeration(getLocaleList());
        }

        private List<Locale> getLocaleList() {
            if (localeList == null) {
                List<Locale> resolved = localeResolver.resolveLocale(this.getSlingRequest());
                this.localeList = (resolved != null && !resolved.isEmpty())
                        ? resolved
                        : Collections.singletonList(this.bundleProvider.getDefaultLocale());
            }

            return localeList;
        }
    }

}
