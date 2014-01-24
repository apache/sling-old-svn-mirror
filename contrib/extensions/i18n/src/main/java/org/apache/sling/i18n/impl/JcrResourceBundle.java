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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceBundle extends ResourceBundle {

    private static final Logger log = LoggerFactory.getLogger(JcrResourceBundle.class);

    static final String JCR_PATH = "jcr:path";

    static final String PROP_KEY = "sling:key";

    static final String PROP_VALUE = "sling:message";

    static final String PROP_BASENAME = "sling:basename";

    static final String PROP_LANGUAGE = "jcr:language";

    /**
     * java.util.Formatter pattern to build the XPath query to search for
     * messages in a given root path (%s argument).
     *
     * @see #loadFully(ResourceResolver, Set, Set)
     */
    private static final String QUERY_MESSAGES_FORMAT = "/jcr:root%s//element(*,sling:Message)";

    private final Map<String, Object> resources;

    private final Locale locale;

    private final Set<String> languageRoots = new HashSet<String>();

    JcrResourceBundle(Locale locale, String baseName,
            ResourceResolver resourceResolver) {
        this.locale = locale;

        long start = System.currentTimeMillis();
        refreshSession(resourceResolver, true);
        Set<String> roots = loadPotentialLanguageRoots(resourceResolver, locale, baseName);
        this.resources = loadFully(resourceResolver, roots, this.languageRoots);
        long end = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug(
                "JcrResourceBundle: Fully loaded {} entries for {} (base: {}) in {}ms",
                new Object[] { resources.size(), locale, baseName,
                    (end - start) });
            log.debug("JcrResourceBundle: Language roots: {}", languageRoots);
        }
    }

    static void refreshSession(ResourceResolver resolver, boolean keepChanges) {
        final Session s = resolver.adaptTo(Session.class);
        if(s == null) {
            log.warn("ResourceResolver {} does not adapt to Session, cannot refresh", resolver);
        } else {
            try {
                s.refresh(keepChanges);
            } catch(RepositoryException re) {
                throw new RuntimeException("RepositoryException in session.refresh", re);
            }
        }
    }

    protected Set<String> getLanguageRootPaths() {
        return languageRoots;
    }

    @Override
    protected void setParent(ResourceBundle parent) {
        super.setParent(parent);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns a Set of all resource keys provided by this resource bundle only.
     * <p>
     * This method is a new Java 1.6 method to implement the
     * ResourceBundle.keySet() method.
     *
     * @return The keys of the resources provided by this resource bundle
     */
    @Override
    protected Set<String> handleKeySet() {
        return resources.keySet();
    }

    @Override
    public Enumeration<String> getKeys() {
        Enumeration<String> parentKeys = (parent != null)
                ? parent.getKeys()
                : null;
        return new ResourceBundleEnumeration(resources.keySet(), parentKeys);
    }

    @Override
    protected Object handleGetObject(String key) {
        return resources.get(key);
    }

    /**
     * Fully loads the resource bundle from the storage.
     * <p>
     * This method adds entries to the {@code languageRoots} set of strings.
     * Therefore this method must not be called concurrently or the set
     * must either be thread safe.
     *
     * @param resourceResolver The storage access (must not be {@code null})
     * @param roots The set of (potential) disctionary subtrees. This must
     *      not be {@code null}. If empty, no resources will actually be
     *      loaded.
     * @param languageRoots The set of actualy dictionary subtrees. While
     *      processing the resources, all subtrees listed in the {@code roots}
     *      set is added to this set if it actually contains resources. This
     *      must not be {@code null}.
     * @return
     *
     * @throws NullPointerException if either of the parameters is {@code null}.
     */
    @SuppressWarnings("deprecation")
    private Map<String, Object> loadFully(final ResourceResolver resourceResolver, Set<String> roots, Set<String> languageRoots) {
        final Map<String, Object> rest = new HashMap<String, Object>();
        for (String root: roots) {
            String fullLoadQuery = String.format(QUERY_MESSAGES_FORMAT, ISO9075.encodePath(root));

            log.debug("Executing full load query {}", fullLoadQuery);

            // do an XPath query because this won't go away soon and still
            // (2011/04/04) is the fastest query language ...
            Iterator<Map<String, Object>> bundles = null;
            try {
                bundles = resourceResolver.queryResources(fullLoadQuery, Query.XPATH);
            } catch (final SlingException se) {
                log.error("Exception during resource query " + fullLoadQuery, se);
            }

            if ( bundles != null ) {
                final String[] path = resourceResolver.getSearchPath();

                final List<Map<String, Object>> res0 = new ArrayList<Map<String, Object>>();
                for (int i = 0; i < path.length; i++) {
                    res0.add(new HashMap<String, Object>());
                }

                while (bundles.hasNext()) {
                    final Map<String, Object> row = bundles.next();
                    if (row.containsKey(PROP_VALUE)) {
                        final String jcrPath = (String) row.get(JCR_PATH);
                        String key = (String) row.get(PROP_KEY);

                        if (key == null) {
                            key = ResourceUtil.getName(jcrPath);
                        }

                        Map<String, Object> dst = rest;
                        for (int i = 0; i < path.length; i++) {
                            if (jcrPath.startsWith(path[i])) {
                                dst = res0.get(i);
                                break;
                            }
                        }

                        dst.put(key, row.get(PROP_VALUE));
                    }
                }

                for (int i = path.length - 1; i >= 0; i--) {
                    final Map<String, Object> resources = res0.get(i);
                    if (!resources.isEmpty()) {
                        rest.putAll(resources);
                        // also remember root
                        languageRoots.add(root);

                    }
                }
            }
        }

        return rest;
    }

    private Set<String> loadPotentialLanguageRoots(ResourceResolver resourceResolver, Locale locale, String baseName) {
        final String localeString = locale.toString();
        final String localeStringLower = localeString.toLowerCase();
        final String localeRFC4646String = toRFC4646String(locale);
        final String localeRFC4646StringLower = localeRFC4646String.toLowerCase();

        Set<String> paths = new HashSet<String>();
        @SuppressWarnings("deprecation")
        Iterator<Resource> bundles = resourceResolver.findResources("//element(*,mix:language)", Query.XPATH);
        while (bundles.hasNext()) {
            Resource bundle = bundles.next();
            ValueMap properties = bundle.adaptTo(ValueMap.class);
            String language = properties.get(PROP_LANGUAGE, String.class);
            if (language != null && language.length() > 0) {
                if (language.equals(localeString)
                        || language.equals(localeStringLower)
                        || language.equals(localeRFC4646String)
                        || language.equals(localeRFC4646StringLower)) {

                    if (baseName == null || baseName.equals(properties.get(PROP_BASENAME, ""))) {
                        paths.add(bundle.getPath());
                    }
                }
            }
        }
        return Collections.unmodifiableSet(paths);
    }

    // Would be nice if Locale.toString() output RFC 4646, but it doesn't
    private static String toRFC4646String(Locale locale) {
        return locale.toString().replace('_', '-');
    }
}
