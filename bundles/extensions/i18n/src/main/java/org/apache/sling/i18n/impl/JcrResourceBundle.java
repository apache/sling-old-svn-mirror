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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.jackrabbit.commons.json.JsonHandler;
import org.apache.jackrabbit.commons.json.JsonParser;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceBundle extends ResourceBundle {

    private static final Logger log = LoggerFactory.getLogger(JcrResourceBundle.class);

    /** default primary type (=resource type) for message entry dictionaries */
    static final String RT_MESSAGE_ENTRY = "sling:MessageEntry";

    static final String MIXIN_MESSAGE = "sling:Message";

    static final String MIXIN_LANGUAGE = "mix:language";

    static final String PROP_KEY = "sling:key";

    static final String PROP_VALUE = "sling:message";

    static final String PROP_BASENAME = "sling:basename";

    static final String PROP_LANGUAGE = "jcr:language";

    static final String PROP_MIXINS = "jcr:mixinTypes";

    static final String QUERY_LANGUAGE_ROOTS = "//element(*,mix:language)[@jcr:language]";

    private final Map<String, Object> resources;

    private final Locale locale;

    private final String baseName;

    private final Set<String> languageRoots = new HashSet<String>();

    JcrResourceBundle(Locale locale, String baseName,
            ResourceResolver resourceResolver) {
        this.locale = locale;
        this.baseName = baseName;

        log.info("Finding all dictionaries for '{}' (basename: {}) ...", locale, baseName == null ? "<none>" : baseName);

        long start = System.currentTimeMillis();
        resourceResolver.refresh();
        Set<String> roots = loadPotentialLanguageRoots(resourceResolver, locale, baseName);
        this.resources = loadFully(resourceResolver, roots, this.languageRoots);

        long end = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            log.info(
                "Finished loading {} entries for '{}' (basename: {}) in {}ms",
                new Object[] { resources.size(), locale, baseName == null ? "<none>" : baseName, (end - start)}
            );
        }
    }

    protected Set<String> getLanguageRootPaths() {
        return languageRoots;
    }

    @Override
    protected void setParent(ResourceBundle parent) {
        super.setParent(parent);
    }

    public ResourceBundle getParent() {
        return parent;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public String getBaseName() {
        return baseName;
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
        if (log.isDebugEnabled()) {
            log.debug("Requesting key '{}' from resource bundle (baseName '{}', locale '{}')", new Object[] {key, baseName, locale});
        }
        return resources.get(key);
    }

    /**
     * Fully loads the resource bundle from the storage.
     * <p>
     * This method adds entries to the {@code languageRoots} set of strings.
     * Therefore this method must not be called concurrently or the set
     * must either be thread safe.
     *
     * @param resolver The storage access (must not be {@code null})
     * @param roots The set of (potential) dictionary subtrees. This must
     *      not be {@code null}. If empty, no resources will actually be
     *      loaded.
     * @param languageRoots The set of actually dictionary subtrees. While
     *      processing the resources, all subtrees listed in the {@code roots}
     *      set is added to this set if it actually contains resources. This
     *      must not be {@code null}.
     * @return
     *
     * @throws NullPointerException if either of the parameters is {@code null}.
     */
    private Map<String, Object> loadFully(final ResourceResolver resolver, Set<String> roots, Set<String> languageRoots) {

        final String[] searchPath = resolver.getSearchPath();

        // for each search path entry, have a list of maps (dictionaries)
        // plus other = "outside the search path" at the end

        //   [0] /apps2  -> [dict1, dict2, dict3 ...]
        //   [1] /apps   -> [dict4, dict5, ...]
        //   [2] /libs   -> [dict6, ...]
        //   [3] (other) -> [dict7, dict8 ...]

        List<List<Map<String, Object>>> dictionariesBySearchPath = new ArrayList<List<Map<String, Object>>>(searchPath.length + 1);
        for (int i = 0; i < searchPath.length + 1; i++) {
            dictionariesBySearchPath.add(new ArrayList<Map<String, Object>>());
        }

        for (final String root: roots) {

            Resource dictionaryResource = resolver.getResource(root);
            if (dictionaryResource == null) {
                log.warn("Dictionary root found by search not accessible: {}", root);
                continue;
            }

            // linked hash map to keep order (not functionally important, but helpful for dictionary debugging)
            Map<String, Object> dictionary = new LinkedHashMap<String, Object>();

            // find where in the search path this dict belongs
            // otherwise put it in the outside-the-search-path bucket (last list)
            List<Map<String, Object>> targetList = dictionariesBySearchPath.get(searchPath.length);
            for (int i = 0; i < searchPath.length; i++) {
                if (root.startsWith(searchPath[i])) {
                    targetList = dictionariesBySearchPath.get(i);
                    break;
                }
            }
            targetList.add(dictionary);

            // check type of dictionary
            if (dictionaryResource.getName().endsWith(".json")) {
                loadJsonDictionary(dictionaryResource, dictionary);
            } else {
                loadSlingMessageDictionary(dictionaryResource, dictionary);
            }

            if (!dictionary.isEmpty()) {
                languageRoots.add(root);
            }
        }

        // linked hash map to keep order (not functionally important, but helpful for dictionary debugging)
        final Map<String, Object> result = new LinkedHashMap<String, Object>();

        // first, add everything that's not under a search path (e.g. /content)
        // below, same strings inside a search path dictionary would overlay them since
        // they are added later to result = overwrite
        for (Map<String, Object> dict : dictionariesBySearchPath.get(searchPath.length)) {
            result.putAll(dict);
        }

        // then, in order of the search path, add all the individual dictionaries into
        // a single result, so that e.g. strings in /apps overlay the ones in /libs
        for (int i = searchPath.length - 1; i >= 0; i--) {

            for (Map<String, Object> dict : dictionariesBySearchPath.get(i)) {
                result.putAll(dict);
            }
        }

        return result;
    }

    private void loadJsonDictionary(Resource resource, final Map<String, Object> targetDictionary) {
        log.info("Loading json dictionary: {}", resource.getPath());

        // use streaming parser (we don't need the dict in memory twice)
        JsonParser parser = new JsonParser(new JsonHandler() {

            private String key;

            @Override
            public void key(String key) throws IOException {
                this.key = key;
            }

            @Override
            public void value(String value) throws IOException {
                targetDictionary.put(key, value);
            }

            @Override
            public void object() throws IOException {}
            @Override
            public void endObject() throws IOException {}
            @Override
            public void array() throws IOException {}
            @Override
            public void endArray() throws IOException {}
            @Override
            public void value(boolean value) throws IOException {}
            @Override
            public void value(long value) throws IOException {}
            @Override
            public void value(double value) throws IOException {}
        });

        final InputStream stream = resource.adaptTo(InputStream.class);
        if (stream != null) {
            String encoding = "utf-8";
            final ResourceMetadata metadata = resource.getResourceMetadata();
            if (metadata.getCharacterEncoding() != null) {
                encoding = metadata.getCharacterEncoding();
            }

            try {

                parser.parse(stream, encoding);

            } catch (IOException e) {
                log.warn("Could not parse i18n json dictionary {}: {}", resource.getPath(), e.getMessage());
            } finally {
                try {
                    stream.close();
                } catch (IOException ignore) {
                }
            }
        } else {
            log.warn("Not a json file: {}", resource.getPath());
        }
    }

    /**
     * Depth-first traversal of a resource tree
     */
    private void scanForSlingMessages(final Resource rsrc, final Map<String, Object> targetDictionary) {
        final ValueMap vm = rsrc.adaptTo(ValueMap.class);
        if ( vm != null ) {
            final String value = vm.get(PROP_VALUE, String.class);
            if ( value != null ) {
                final String key = vm.get(PROP_KEY, rsrc.getName());
                targetDictionary.put(key, value);
            }
        }

        for(final Resource c : rsrc.getChildren()) {
            scanForSlingMessages(c, targetDictionary);
        }
    }

    private void loadSlingMessageDictionary(final Resource dictionaryResource, final Map<String, Object> targetDictionary) {
        log.info("Loading sling:Message dictionary: {}", dictionaryResource.getPath());

        this.scanForSlingMessages(dictionaryResource, targetDictionary);
    }

    private Set<String> loadPotentialLanguageRoots(ResourceResolver resourceResolver, Locale locale, String baseName) {
        final String localeString = locale.toString();
        final String localeStringLower = localeString.toLowerCase();
        final String localeRFC4646String = toRFC4646String(locale);
        final String localeRFC4646StringLower = localeRFC4646String.toLowerCase();

        final Set<String> paths = new LinkedHashSet<String>();
        final Iterator<Resource> bundles = resourceResolver.findResources(QUERY_LANGUAGE_ROOTS, "xpath");
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

    @Override
    public String toString() {
        return "JcrResourceBundle [locale=" + locale + ", baseName=" + baseName + ", languageRoots=" + languageRoots
                + ", parent=" + parent + "]";
    }
}
