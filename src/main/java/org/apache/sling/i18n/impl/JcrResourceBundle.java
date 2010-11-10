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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.query.Query;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceBundle extends ResourceBundle {

    private static final Logger log = LoggerFactory.getLogger(JcrResourceBundle.class);

    private static final String JCR_PATH = "jcr:path";

    private static final String PROP_KEY = "sling:key";

    private static final String PROP_VALUE = "sling:message";

    private static final String PROP_BASENAME = "sling:basename";

    /**
     * Search the tree below a mix:language node matching a given language...
     */
    private static final String QUERY_BASE =
        "//element(*,mix:language)[@jcr:language='%s'%s]//element(*,sling:Message)";

    /**
     * ... and find all nodes with a sling:message property set
     * (typically with mixin sling:Message).
     */
    private static final String QUERY_LOAD_FULLY = QUERY_BASE
        + "[@" + PROP_VALUE + "]/(@" + PROP_KEY + "|@" + PROP_VALUE + ")";

    /**
     * ... or find a node with the message (sling:message property) for
     * a given key (sling:key property). Also find nodes without sling:key
     * property and examine their node name.
     */
    private static final String QUERY_LOAD_RESOURCE = QUERY_BASE + "[@"
        + PROP_KEY + "='%s' or not(@" + PROP_KEY + ")]/(@" + PROP_KEY + "|@" + PROP_VALUE + ")";

    private final ResourceResolver resourceResolver;

    private final ConcurrentHashMap<String, Object> resources;

    private boolean fullyLoaded;

    private final Locale locale;

    private final String baseName;

    private String[] searchPath;

    JcrResourceBundle(Locale locale, String baseName,
            ResourceResolver resourceResolver) {
        this.locale = locale;
        this.baseName = baseName;
        this.resourceResolver = resourceResolver;
        this.resources = new ConcurrentHashMap<String, Object>();
        this.fullyLoaded = false;
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
    protected Set<String> handleKeySet() {
        // ensure all keys are loaded
        loadFully();

        return resources.keySet();
    }

    @Override
    public Enumeration<String> getKeys() {
        // ensure all keys are loaded
        loadFully();

        Enumeration<String> parentKeys = (parent != null)
                ? parent.getKeys()
                : null;
        return new ResourceBundleEnumeration(resources.keySet(), parentKeys);
    }

    @Override
    protected Object handleGetObject(String key) {
        Object value = resources.get(key);
        if (value == null && !fullyLoaded) {
            value = loadResource(key);
            if (value != null) {
                resources.put(key, value);
            }
        }

        return (value != null) ? value : key;
    }

    private void loadFully() {
        if (!fullyLoaded) {

            synchronized (this) {
                if (!fullyLoaded) {

                    final String fullLoadQuery = getFullLoadQuery();
                    if (log.isDebugEnabled()) {
                        log.debug("Executing full load query {}", fullLoadQuery);
                    }
                    Iterator<Map<String, Object>> bundles = resourceResolver.queryResources(
                        fullLoadQuery, Query.XPATH);

                    String[] path = getSearchPath();

                    List<Map<String, Object>> res0 = new ArrayList<Map<String, Object>>();
                    for (int i = 0; i < path.length; i++) {
                        res0.add(new HashMap<String, Object>());
                    }
                    Map<String, Object> rest = new HashMap<String, Object>();

                    while (bundles.hasNext()) {
                        Map<String, Object> row = bundles.next();
                        String jcrPath = (String) row.get(JCR_PATH);
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

                    for (int i = path.length - 1; i >= 0; i--) {
                        rest.putAll(res0.get(i));
                    }

                    resources.putAll(rest);

                    fullyLoaded = true;
                }
            }
        }
    }

    private Object loadResource(String key) {
        final String resourceQuery = getResourceQuery(key);
        if (log.isDebugEnabled()) {
            log.debug("Executing resource query {}", resourceQuery);
        }
        Iterator<Map<String, Object>> bundles = resourceResolver
                .queryResources(resourceQuery, Query.XPATH);
        if (bundles.hasNext()) {

            String[] path = getSearchPath();

            Map<String, Object> currentValue = null;
            int currentWeight = path.length;

            while (bundles.hasNext() && currentWeight > 0) {
                Map<String, Object> resource = bundles.next();
                String jcrPath = (String) resource.get(JCR_PATH);

                // skip resources without sling:key and non-matching nodename
                if (resource.get(PROP_KEY) == null) {
                    if (!key.equals(Text.getName(jcrPath))) {
                        continue;
                    }
                }

                for (int i = 0; i < currentWeight; i++) {
                    if (jcrPath.startsWith(path[i])) {
                        currentWeight = i;
                        currentValue = resource;
                        break;
                    }
                }

                // the path is not listed, check the current resource if
                // none has yet been set
                if (currentValue == null) {
                    currentValue = resource;
                }
            }

            if (currentValue != null) {
                return currentValue.get(PROP_VALUE);
            }
        }

        return null;
    }

    private String getFullLoadQuery() {
        return String.format(QUERY_LOAD_FULLY, getLocale(), getBaseNameTerm());
    }

    private String getResourceQuery(String key) {
        return String.format(QUERY_LOAD_RESOURCE, getLocale(),
            getBaseNameTerm(), key.replace("'", "''"));
    }

    private String getBaseNameTerm() {
        if (baseName == null) {
            return "";
        }

        StringBuilder buf = new StringBuilder(" and @");
        buf.append(PROP_BASENAME);

        if (baseName.length() > 0) {
            buf.append("='").append(baseName).append('\'');
        }

        return buf.toString();
    }

    private String[] getSearchPath() {
        if (searchPath == null) {
            searchPath = resourceResolver.getSearchPath();
        }

        return searchPath;
    }
}
