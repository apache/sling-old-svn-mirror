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

import static org.apache.sling.i18n.impl.JcrResourceBundle.PROP_BASENAME;
import static org.apache.sling.i18n.impl.JcrResourceBundle.PROP_LANGUAGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceBundleProvider</code> implements the
 * <code>ResourceBundleProvider</code> interface creating
 * <code>ResourceBundle</code> instances from resources stored in the
 * repository.
 */
@Component(immediate = true, metatype = true, label = "%provider.name", description = "%provider.description")
@Service({ResourceBundleProvider.class, ResourceChangeListener.class})
@Property(name=ResourceChangeListener.PATHS, value="/")
public class JcrResourceBundleProvider implements ResourceBundleProvider, ResourceChangeListener, ExternalResourceChangeListener {

    private static final boolean DEFAULT_PRELOAD_BUNDLES = false;

    private static final int DEFAULT_INVALIDATION_DELAY = 5000;

    @Property(value = "")
    private static final String PROP_USER = "user";

    @Property(value = "")
    private static final String PROP_PASS = "password";

    @Property(value = "en")
    private static final String PROP_DEFAULT_LOCALE = "locale.default";

    @Property(boolValue = DEFAULT_PRELOAD_BUNDLES)
    private static final String PROP_PRELOAD_BUNDLES = "preload.bundles";

    @Property(longValue = DEFAULT_INVALIDATION_DELAY)
    private static final String PROP_INVALIDATION_DELAY = "invalidation.delay";

    @Reference
    private Scheduler scheduler;

    /** job names of scheduled jobs for reloading individual bundles */
    private final Collection<String> scheduledJobNames = Collections.synchronizedList(new ArrayList<String>()) ;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * The default Locale as configured with the <i>locale.default</i>
     * configuration property. This defaults to <code>Locale.ENGLISH</code> if
     * the configuration property is not set.
     */
    private Locale defaultLocale = Locale.ENGLISH;

    /**
     * The resource resolver used to access the resource bundles. This object is
     * retrieved from the {@link #resourceResolverFactory} using the administrative
     * session or the session acquired using the {@link #repoCredentials}.
     */
    private ResourceResolver resourceResolver;

    /**
     * Map of cached resource bundles indexed by a key combined of the base name
     * and <code>Locale</code> used to load and identify the <code>ResourceBundle</code>.
     */
    private final ConcurrentHashMap<Key, JcrResourceBundle> resourceBundleCache = new ConcurrentHashMap<Key, JcrResourceBundle>();

    private final ConcurrentHashMap<Key, Semaphore> loadingGuards = new ConcurrentHashMap<Key, Semaphore>();

    /**
     * paths from which JCR resource bundles have been loaded
     */
    private final Set<String> languageRootPaths = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Return root resource bundle as created on-demand by
     * {@link #getRootResourceBundle()}.
     */
    private ResourceBundle rootResourceBundle;

    private BundleContext bundleContext;

    /**
     * Each ResourceBundle is registered as a service. Each registration is stored in this map with the locale & base name used as a key.
     */
    private final Map<Key, ServiceRegistration<ResourceBundle>> bundleServiceRegistrations = new HashMap<Key, ServiceRegistration<ResourceBundle>>();

    private boolean preloadBundles;

    private long invalidationDelay;

    // ---------- ResourceBundleProvider ---------------------------------------

    /**
     * Returns the configured default <code>Locale</code> which is used as a
     * fallback for {@link #getResourceBundle(Locale)} and also as the basis for
     * any messages requested from resource bundles.
     */
    @Override
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
    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return getResourceBundle(null, locale);
    }

    @Override
    public ResourceBundle getResourceBundle(String baseName, Locale locale) {
        if (locale == null) {
            locale = defaultLocale;
        }

        return getResourceBundleInternal(baseName, locale);
    }

    // ---------- EventHandler ------------------------------------------------

    @Override
    public void onChange(List<ResourceChange> changes) {
        for(final ResourceChange change : changes) {
            log.trace("handleEvent: Detecting event {} for path '{}'", change.getType(), change.getPath());

            // if this change was on languageRootPath level this might change basename and locale as well, therefore
            // invalidate everything
            if (languageRootPaths.contains(change.getPath())) {
                log.debug(
                        "handleEvent: Detected change of cached language root '{}', removing all cached ResourceBundles",
                        change.getPath());
                scheduleReloadBundles(true);
            } else {
                // if it is only a change below a root path, only messages of one resource bundle can be affected!
                for (final String root : languageRootPaths) {
                    if (change.getPath().startsWith(root)) {
                        // figure out which JcrResourceBundle from the cached ones is affected
                        for (JcrResourceBundle bundle : resourceBundleCache.values()) {
                            if (bundle.getLanguageRootPaths().contains(root)) {
                                // reload it
                                log.debug("handleEvent: Resource changes below '{}', reloading ResourceBundle '{}'",
                                        root, bundle);
                                scheduleReloadBundle(bundle);
                                return;
                            }
                        }
                        log.debug("handleEvent: No cached resource bundle found with root '{}'", root);
                        break;
                    }
                }
                // may be a completely new dictionary
                if (isDictionaryResource(change)) {
                    scheduleReloadBundles(true);
                }
            }
        }
    }

    private boolean isDictionaryResource(final ResourceChange change) {
        // language node changes happen quite frequently (https://issues.apache.org/jira/browse/SLING-2881)
        // therefore only consider changes either for sling:MessageEntry's
        // or for JSON dictionaries
        // get valuemap
        resourceResolver.refresh();
        final Resource resource = resourceResolver.getResource(change.getPath());
        if (resource == null) {
            log.trace("Could not get resource for '{}' for event {}", change.getPath(), change.getType());
            return false;
        }
        if ( resource.getResourceType() == null ) {
            return false;
        }
        if (resource.isResourceType(JcrResourceBundle.RT_MESSAGE_ENTRY)) {
            log.debug("Found new dictionary entry: New {} resource in '{}' detected", JcrResourceBundle.RT_MESSAGE_ENTRY, change.getPath());
            return true;
        }
        final ValueMap valueMap = resource.getValueMap();
        // FIXME: derivatives from mix:Message are not detected
        if (hasMixin(valueMap, JcrResourceBundle.MIXIN_MESSAGE)) {
            log.debug("Found new dictionary entry: New {} resource in '{}' detected", JcrResourceBundle.MIXIN_MESSAGE, change.getPath());
            return true;
        }
        if (change.getPath().endsWith(".json")) {
            // check for mixin
            if (hasMixin(valueMap, JcrResourceBundle.MIXIN_LANGUAGE)) {
                log.debug("Found new dictionary: New {} resource in '{}' detected", JcrResourceBundle.MIXIN_LANGUAGE, change.getPath());
                return true;
            }
        }
        return false;
    }

    private boolean hasMixin(ValueMap valueMap, String mixin) {
        final String[] mixins = valueMap.get(JcrResourceBundle.PROP_MIXINS, String[].class);
        if ( mixins != null ) {
            for(final String m : mixins) {
                if (mixin.equals(m) ) {
                    return true;
                }
            }
        }
        return false;
    }

    private void scheduleReloadBundles(boolean withDelay) {
        // cancel all reload individual bundle jobs!
        synchronized(scheduledJobNames) {
            for (String scheduledJobName : scheduledJobNames) {
                scheduler.unschedule(scheduledJobName);
            }
        }
        scheduledJobNames.clear();
        // defer this job
        final ScheduleOptions options;
        if (withDelay) {
            options = scheduler.AT(new Date(System.currentTimeMillis() + invalidationDelay));
        } else {
            options = scheduler.NOW();
        }
        options.name("JcrResourceBundleProvider: reload all resource bundles");
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                log.info("Reloading all resource bundles");
                clearCache();
                preloadBundles();
            }
        }, options);
    }

    private void scheduleReloadBundle(JcrResourceBundle bundle) {
        String baseName = bundle.getBaseName();
        Locale locale = bundle.getLocale();
        final Key key = new Key(baseName, locale);

        // defer this job
        ScheduleOptions options = scheduler.AT(new Date(System.currentTimeMillis() + invalidationDelay));
        final String jobName = "JcrResourceBundleProvider: reload bundle with key " + key.toString();
        scheduledJobNames.add(jobName);
        options.name(jobName);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                reloadBundle(key);
                scheduledJobNames.remove(jobName);
            }
        }, options);
    }

    void reloadBundle(final Key key) {
        // remove bundle from cache
        resourceBundleCache.remove(key);
        log.info("Reloading resource bundle for {}", key);
        // unregister bundle
        ServiceRegistration<ResourceBundle> serviceRegistration = null;
        synchronized (this) {
            serviceRegistration = bundleServiceRegistrations.remove(key);
        }
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        } else {
            log.warn("Could not find resource bundle service for {}", key);
        }

        Collection<JcrResourceBundle> dependentBundles = new ArrayList<JcrResourceBundle>();
        // this bundle might be a parent of a cached bundle -> invalidate those dependent bundles as well
        for (JcrResourceBundle bundle : resourceBundleCache.values()) {
            if (bundle.getParent() instanceof JcrResourceBundle) {
                JcrResourceBundle parentBundle = (JcrResourceBundle) bundle.getParent();
                Key parentKey = new Key(parentBundle.getBaseName(), parentBundle.getLocale());
                if (parentKey.equals(key)) {
                    log.debug("Also invalidate dependent bundle {} which has bundle {} as parent", bundle, parentBundle);
                    dependentBundles.add(bundle);
                }
            }
        }
        for (JcrResourceBundle dependentBundle : dependentBundles) {
            reloadBundle(new Key(dependentBundle.getBaseName(), dependentBundle.getLocale()));
        }

        if (preloadBundles) {
            // reload the bundle from the repository (will also fill cache and register as a service)
            getResourceBundle(key.baseName, key.locale);
        }
    }

    // ---------- SCR Integration ----------------------------------------------

    /**
     * Activates and configures this component with the repository access
     * details and the default locale to use
     * @throws LoginException
     */
    protected void activate(BundleContext context, Map<String, Object> props) throws LoginException {
        Map<String, Object> repoCredentials;
        String user = PropertiesUtil.toString(props.get(PROP_USER), null);
        if (user == null || user.length() == 0) {
            repoCredentials = null;
        } else {
            String pass = PropertiesUtil.toString(props.get(PROP_PASS), null);
            char[] pwd = (pass == null) ? new char[0] : pass.toCharArray();
            repoCredentials = new HashMap<String, Object>();
            repoCredentials.put(ResourceResolverFactory.USER, user);
            repoCredentials.put(ResourceResolverFactory.PASSWORD, pwd);
        }

        String localeString = PropertiesUtil.toString(props.get(PROP_DEFAULT_LOCALE),
            null);
        this.defaultLocale = toLocale(localeString);
        this.preloadBundles = PropertiesUtil.toBoolean(props.get(PROP_PRELOAD_BUNDLES), DEFAULT_PRELOAD_BUNDLES);

        this.bundleContext = context;
        invalidationDelay = PropertiesUtil.toLong(props.get(PROP_INVALIDATION_DELAY), DEFAULT_INVALIDATION_DELAY);
        if (this.resourceResolverFactory != null) { // this is only null during test execution!
            if (repoCredentials == null) {
                resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            } else {
                resourceResolver = resourceResolverFactory.getResourceResolver(repoCredentials);
            }
            scheduleReloadBundles(false);
        }

    }

    protected void deactivate() {
        clearCache();
        resourceResolver.close();
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
    private ResourceBundle getResourceBundleInternal(final String baseName, final Locale locale) {
        final Key key = new Key(baseName, locale);
        JcrResourceBundle resourceBundle = resourceBundleCache.get(key);
        if (resourceBundle != null) {
            log.debug("getResourceBundleInternal({}): got cache hit on first try", key);
        } else {
            if (loadingGuards.get(key) == null) {
                loadingGuards.putIfAbsent(key, new Semaphore(1));
            }
            final Semaphore loadingGuard = loadingGuards.get(key);
            try {
                loadingGuard.acquire();
                resourceBundle = resourceBundleCache.get(key);
                if (resourceBundle != null) {
                    log.debug("getResourceBundleInternal({}): got cache hit on second try", key);
                } else {
                    log.debug("getResourceBundleInternal({}): reading from Repository", key);
                    resourceBundle = createResourceBundle(key.baseName, key.locale);
                    resourceBundleCache.put(key, resourceBundle);
                    registerResourceBundle(key, resourceBundle);
                }
            } catch (InterruptedException e) {
                Thread.interrupted();
            } finally {
                loadingGuard.release();
            }
        }
        log.trace("getResourceBundleInternal({}) ==> {}", key, resourceBundle);
        return resourceBundle;
    }

    private void registerResourceBundle(Key key, JcrResourceBundle resourceBundle) {
        Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
        if (key.baseName != null) {
            serviceProps.put("baseName", key.baseName);
        }
        serviceProps.put("locale", key.locale.toString());
        ServiceRegistration<ResourceBundle> serviceReg = bundleContext.registerService(ResourceBundle.class,
                resourceBundle, serviceProps);
        synchronized (this) {
            bundleServiceRegistrations.put(key, serviceReg);
        }

        // register language root paths
        final Set<String> languageRoots = resourceBundle.getLanguageRootPaths();
        this.languageRootPaths.addAll(languageRoots);

        log.debug("registerResourceBundle({}, ...): added service registration and language roots {}", key, languageRoots);
        log.info("Currently loaded dictionaries across all locales: {}", languageRootPaths);
    }

    /**
     * Creates the resource bundle for the give locale.
     *
     * @throws MissingResourceException If the <code>ResourceResolver</code>
     *             is not available to access the resources.
     */
    private JcrResourceBundle createResourceBundle(String baseName, Locale locale) {
        final JcrResourceBundle bundle = new JcrResourceBundle(locale, baseName, resourceResolver);

        // set parent resource bundle
        Locale parentLocale = getParentLocale(locale);
        if (parentLocale != null) {
            bundle.setParent(getResourceBundleInternal(baseName, parentLocale));
        } else {
            bundle.setParent(getRootResourceBundle());
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
     * Returns a ResourceBundle which is used as the root resource bundle, that
     * is the ultimate parent:
     * <ul>
     * <li><code>getLocale()</code> returns Locale("", "", "")</li>
     * <li><code>handleGetObject(String key)</code> returns the <code>key</code></li>
     * <li><code>getKeys()</code> returns an empty enumeration.
     * </ul>
     *
     * @return The root resource bundle
     */
    private ResourceBundle getRootResourceBundle() {
        if (rootResourceBundle == null) {
            rootResourceBundle = new RootResourceBundle();
        }
        return rootResourceBundle;
    }

    private void clearCache() {
        resourceBundleCache.clear();
        languageRootPaths.clear();

        synchronized (this) {
            for (ServiceRegistration<ResourceBundle> serviceReg : bundleServiceRegistrations.values()) {
                serviceReg.unregister();
            }
            bundleServiceRegistrations.clear();
        }
    }

    private void preloadBundles() {
        if (preloadBundles) {
            resourceResolver.refresh();
            Iterator<Map<String, Object>> bundles = resourceResolver.queryResources(
                    JcrResourceBundle.QUERY_LANGUAGE_ROOTS, "xpath");
            Set<Key> usedKeys = new HashSet<Key>();
            while (bundles.hasNext()) {
                Map<String,Object> bundle = bundles.next();
                if (bundle.containsKey(PROP_LANGUAGE)) {
                    Locale locale = toLocale(bundle.get(PROP_LANGUAGE).toString());
                    String baseName = null;
                    if (bundle.containsKey(PROP_BASENAME)) {
                        baseName = bundle.get(PROP_BASENAME).toString();
                    }
                    Key key = new Key(baseName, locale);
                    if (usedKeys.add(key)) {
                        getResourceBundle(baseName, locale);
                    }
                }
            }
        }
    }

    /**
     * Converts the given <code>localeString</code> to a valid
     * <code>java.util.Locale</code>. It must either be in the format specified by
     * {@link Locale#toString()} or in <a href="https://tools.ietf.org/html/bcp47">BCP 47 format</a>
     * If the locale string is <code>null</code> or empty, the platform default locale is assumed. If
     * the localeString matches any locale available per default on the
     * platform, that platform locale is returned. Otherwise the localeString is
     * parsed and the language and country parts are compared against the
     * languages and countries provided by the platform. Any unsupported
     * language or country is replaced by the platform default language and
     * country.
     * @param localeString the locale as string
     * @return the {@link Locale} being generated from the {@code localeString}
     */
    static Locale toLocale(String localeString) {
        if (localeString == null || localeString.length() == 0) {
            return Locale.getDefault();
        }
        // support BCP 47 compliant strings as well (using a different separator "-" instead of "_")
        localeString = localeString.replaceAll("-", "_");

        // check language and country
        final String[] parts = localeString.split("_");
        if (parts.length == 0) {
            return Locale.getDefault();
        }

        // at least language is available
        String lang = parts[0];
        boolean isValidLanguageCode = false;
        String[] langs = Locale.getISOLanguages();
        for (int i = 0; i < langs.length; i++) {
            if (langs[i].equalsIgnoreCase(lang)) {
                isValidLanguageCode = true;
                break;
            }
        }
        if (!isValidLanguageCode) {
            lang = Locale.getDefault().getLanguage();
        }

        // only language
        if (parts.length == 1) {
            return new Locale(lang);
        }

        // country is also available
        String country = parts[1];
        boolean isValidCountryCode = false;
        String[] countries = Locale.getISOCountries();
        for (int i = 0; i < countries.length; i++) {
            if (countries[i].equalsIgnoreCase(country)) {
                isValidCountryCode = true; // signal ok
                break;
            }
        }
        if (!isValidCountryCode) {
            country = Locale.getDefault().getCountry();
        }

        // language and country
        if (parts.length == 2) {
            return new Locale(lang, country);
        }

        // language, country and variant
        return new Locale(lang, country, parts[2]);
    }

    //---------- internal class

    /**
     * The <code>Key</code> class encapsulates the base name and Locale in a
     * single object that can be used as the key in a <code>HashMap</code>.
     */
    protected static final class Key {

        final String baseName;

        final Locale locale;

        // precomputed hash code, because this will always be used due to
        // this instance being used as a key in a HashMap.
        private final int hashCode;

        Key(final String baseName, final Locale locale) {

            int hc = 0;
            if (baseName != null) {
                hc += 17 * baseName.hashCode();
            }
            if (locale != null) {
                hc += 13 * locale.hashCode();
            }

            this.baseName = baseName;
            this.locale = locale;
            this.hashCode = hc;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Key) {
                Key other = (Key) obj;
                return equals(this.baseName, other.baseName)
                    && equals(this.locale, other.locale);
            }

            return false;
        }

        private static boolean equals(Object o1, Object o2) {
            if (o1 == null) {
                if (o2 != null) {
                    return false;
                }
            } else if (!o1.equals(o2)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Key(" + baseName + ", " + locale + ")";
        }
    }
}
