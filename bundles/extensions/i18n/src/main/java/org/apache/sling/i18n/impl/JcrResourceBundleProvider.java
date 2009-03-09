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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceBundleProvider</code> implements the
 * <code>ResourceBundleProvider</code> interface creating
 * <code>ResourceBundle</code> instances from resources stored in the
 * repository.
 *
 * @scr.component immediate="true" label="%provider.name"
 *                description="%provider.description"
 * @scr.service interface="org.apache.sling.i18n.ResourceBundleProvider"
 */
public class JcrResourceBundleProvider implements ResourceBundleProvider,
        EventListener {

    /** @scr.property value="" */
    private static final String PROP_USER = "user";

    /** @scr.property value="" */
    private static final String PROP_PASS = "password";

    /** @scr.property value="en" */
    private static final String PROP_DEFAULT_LOCALE = "locale.default";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private SlingRepository repository;

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private JcrResourceResolverFactory resourceResolverFactory;

    /**
     * The default Locale as configured with the <i>locale.default</i>
     * configuration property. This defaults to <code>Locale.ENGLISH</code> if
     * the configuration property is not set.
     */
    private Locale defaultLocale = Locale.ENGLISH;

    /**
     * The credentials to access the repository or <code>null</code> to use
     * access the repository as the anonymous user, which is the case if the
     * <i>user</i> property is not set in the configuration.
     */
    private Credentials repoCredentials;

    /**
     * The resource resolver used to access the resource bundles. This object is
     * retrieved from the {@link #resourceResolverFactory} using the anonymous
     * session or the session acquired using the {@link #repoCredentials}.
     */
    private ResourceResolver resourceResolver;

    /**
     * Matrix of cached resource bundles. The first key is the resource bundle
     * base name, the second key is the Locale.
     */
    private final Map<String, Map<Locale, ResourceBundle>> resourceBundleCache = new HashMap<String, Map<Locale, ResourceBundle>>();

    // ---------- ResourceBundleProvider ---------------------------------------

    /**
     * Returns the configured default <code>Locale</code> which is used as a
     * fallback for {@link #getResourceBundle(Locale)} and also as the basis for
     * any messages requested from resource bundles.
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Returns the <code>ResourceBundle</code> for the given
     * <code>locale</code>.
     *
     * @param locale The <code>Locale</code> for which to return the resource
     *            bundle. If this is <code>null</code> the configured
     *            {@link #getDefaultLocale() default locale} is assumed.
     * @return The <code>ResourceBundle</code> for the given locale.
     * @throws MissingResourceException If the <code>ResourceResolver</code>
     *             is not available to access the resources.
     */
    public ResourceBundle getResourceBundle(Locale locale) {
        return getResourceBundle(null, locale);
    }

    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        if (locale == null) {
            locale = defaultLocale;
        }

        return getResourceBundleInternal(baseName, locale);
    }

    // ---------- EventListener ------------------------------------------------

    /**
     * Called whenever something is changed inside of <code>jcr:language</code>
     * or <code>sling:Message</code> nodes. We just removed all cached
     * resource bundles in this case to force reloading them.
     * <p>
     * This is much simpler than analyzing the events and trying to be clever
     * about which exact resource bundles to remove from the cache and at the
     * same time care for any resource bundle dependencies.
     *
     * @param events The actual JCR events are ignored by this implementation.
     */
    public void onEvent(EventIterator events) {
        log.debug("onEvent: Resource changes, removing cached ResourceBundles");
        synchronized (resourceBundleCache) {
            resourceBundleCache.clear();
        }
    }

    // ---------- SCR Integration ----------------------------------------------

    /**
     * Activates and configures this component with the repository access
     * details and the default locale to use
     */
    protected void activate(ComponentContext context) {
        Dictionary<?, ?> props = context.getProperties();

        String user = (String) props.get(PROP_USER);
        if (user == null || user.length() == 0) {
            repoCredentials = null;
        } else {
            String pass = (String) props.get(PROP_PASS);
            char[] pwd = (pass == null) ? new char[0] : pass.toCharArray();
            repoCredentials = new SimpleCredentials(user, pwd);
        }

        String localeString = (String) props.get(PROP_DEFAULT_LOCALE);
        this.defaultLocale = toLocale(localeString);
    }

    /**
     * Binds the new <code>respository</code>. If this new repository
     * replaces and already bound repository, that latter one is released, that
     * is the session we have on it is loggged out.
     */
    protected void bindRepository(SlingRepository repository) {
        if (this.repository != null) {
            releaseRepository();
        }
        this.repository = repository;
    }

    /**
     * Unbinds the given <code>repository</code>. If this is the same
     * repository we are bound to, we release it by logging out the session we
     * may have to it.
     */
    protected void unbindRepository(SlingRepository repository) {
        if (this.repository == repository) {
            releaseRepository();
            this.repository = null;
        }
    }

    /**
     * Binds a new <code>ResourceResolverFactory</code>. If we are already
     * bound to another factory, we release that latter one first.
     */
    protected void bindResourceResolverFactory(
            JcrResourceResolverFactory resourceResolverFactory) {
        if (this.resourceResolverFactory != null) {
            releaseRepository();
        }
        this.resourceResolverFactory = resourceResolverFactory;
    }

    /**
     * Unbinds the <code>ResourceResolverFactory</code>. If we are bound to
     * this factory, we release it.
     */
    protected void unbindResourceResolverFactory(
            JcrResourceResolverFactory resourceResolverFactory) {
        if (this.resourceResolverFactory == resourceResolverFactory) {
            releaseRepository();
            this.resourceResolverFactory = null;
        }
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Internal implementation of the {@link #getResourceBundle(Locale)} method
     * employing the cache of resource bundles. Creates the bundle if not
     * already cached.
     *
     * @throws MissingResourceException If the resource bundles needs to be
     *             created and the <code>ResourceResolver</code> is not
     *             available to access the resources.
     */
    private ResourceBundle getResourceBundleInternal(String baseName,
            Locale locale) {
        ResourceBundle resourceBundle = null;
        synchronized (resourceBundleCache) {
            Map<Locale, ResourceBundle> appBundles = resourceBundleCache.get(baseName);
            if (appBundles != null) {
                resourceBundle = appBundles.get(locale);
            }
        }

        if (resourceBundle == null) {
            resourceBundle = createResourceBundle(baseName, locale);

            synchronized (resourceBundleCache) {
                Map<Locale, ResourceBundle> appBundles = resourceBundleCache.get(baseName);
                if (appBundles == null) {
                    appBundles = new HashMap<Locale, ResourceBundle>();
                    resourceBundleCache.put(baseName, appBundles);
                }

                // while creating the resource bundle, another thread may
                // have created the same and already stored it in the cache.
                // in this case we don't use the one we just created but use
                // the bundle from the cache. Otherwise, we store our bundle
                // in the cache and keep using it.
                if (appBundles.containsKey(locale)) {
                    resourceBundle = appBundles.get(locale);
                } else {
                    appBundles.put(locale, resourceBundle);
                }
            }

        }

        return resourceBundle;
    }

    /**
     * Creates the resource bundle for the give locale.
     *
     * @throws MissingResourceException If the <code>ResourceResolver</code>
     *             is not available to access the resources.
     */
    private ResourceBundle createResourceBundle(String baseName, Locale locale) {

        ResourceResolver resolver = getResourceResolver();
        if (resolver == null) {
            log.info("createResourceBundle: Missing Resource Resolver, cannot create Resource Bundle");
            throw new MissingResourceException(
                "ResourceResolver not available", getClass().getName(), "");
        }

        JcrResourceBundle bundle = new JcrResourceBundle(locale, baseName,
            resolver);

        // set parent resource bundle
        Locale parentLocale = getParentLocale(locale);
        if (parentLocale != null) {
            bundle.setParent(getResourceBundleInternal(baseName, parentLocale));
        }

        return bundle;
    }

    /**
     * Returns the parent locale of the given locale. The parent locale is the
     * locale of a locale is defined as follows:
     * <ol>
     * <li>If the locale has an variant, the parent locale is the locale with
     * the same language and country without the variant.</li>
     * <li>If the locale has no variant but a country, the parent locale is the
     * locale with the same language but neither country nor variant.</li>
     * <li>If the locale has no country and not variant and whose language is
     * different from the language of the the configured default locale, the
     * parent locale is the configured default locale.</li>
     * <li>Otherwise there is no parent locale and <code>null</code> is
     * returned.</li>
     * </ol>
     */
    private Locale getParentLocale(Locale locale) {
        if (locale.getVariant().length() != 0) {
            return new Locale(locale.getLanguage(), locale.getCountry());
        } else if (locale.getCountry().length() != 0) {
            return new Locale(locale.getLanguage());
        } else if (!locale.getLanguage().equals(defaultLocale.getLanguage())) {
            return defaultLocale;
        }

        // no more parents
        return null;
    }

    /**
     * Returns the resource resolver to access messages. This method logs into
     * the repository and registers with the observation manager if not already
     * done so. If unable to connect to the repository, <code>null</code> is
     * returned.
     *
     * @return The <code>ResourceResolver</code> or <code>null</code> if
     *         unable to login to the repository. <code>null</code> is also
     *         returned if no <code>ResourceResolverFactory</code> or no
     *         <code>Repository</code> is available.
     */
    private ResourceResolver getResourceResolver() {
        if (resourceResolver == null) {
            SlingRepository repo = this.repository;
            JcrResourceResolverFactory fac = this.resourceResolverFactory;
            if (repo == null) {

                log.error("getResourceResolver: SlingRepository missing. Cannot login to create ResourceResolver");

            } else if (fac == null) {

                log.error("getResourceResolver: ResourceResolverFactory is missing. Cannot create ResourceResolver");

            } else {
                Session s = null;
                try {
                    if (repoCredentials == null) {
                        s = repo.loginAdministrative(null);
                    } else {
                        s = repo.login(repoCredentials);
                    }

                    ObservationManager om = s.getWorkspace().getObservationManager();
                    om.addEventListener(this, 255, "/", true, null,
                        new String[] { "mix:language", "sling:Message" }, true);

                    resourceResolver = fac.getResourceResolver(s);

                } catch (RepositoryException re) {
                    log.error(
                        "getResourceResolver: Problem setting up ResourceResolver with Session",
                        re);

                } finally {
                    // drop session if we can login but not get the resource
                    // resolver
                    if (resourceResolver == null && s != null) {
                        s.logout();
                    }
                }

            }
        }

        return resourceResolver;
    }

    /**
     * Logs out from the repository and clears the resource bundle cache.
     */
    private void releaseRepository() {
        ResourceResolver resolver = this.resourceResolver;

        this.resourceResolver = null;

        synchronized (resourceBundleCache) {
            this.resourceBundleCache.clear();
        }

        if (resolver != null) {

            Session s = resolver.adaptTo(Session.class);
            if (s != null) {

                try {
                    ObservationManager om = s.getWorkspace().getObservationManager();
                    om.removeEventListener(this);
                } catch (Throwable t) {
                    log.info(
                        "releaseRepository: Problem unregistering as event listener",
                        t);
                }

                try {
                    s.logout();
                } catch (Throwable t) {
                    log.info(
                        "releaseRepository: Unexpected problem logging out from the repository",
                        t);
                }
            }
        }
    }

    /**
     * Converts the given <code>localeString</code> to valid
     * <code>java.util.Locale</code>. If the locale string is
     * <code>null</code> or empty, the platform default locale is assumed. If
     * the localeString matches any locale available per default on the
     * platform, that platform locale is returned. Otherwise the localeString is
     * parsed and the language and country parts are compared against the
     * languages and countries provided by the platform. Any unsupported
     * language or country is replaced by the platform default language and
     * country.
     */
    private Locale toLocale(String localeString) {
        if (localeString == null || localeString.length() == 0) {
            return Locale.getDefault();
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
                country = null; // signal ok
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
