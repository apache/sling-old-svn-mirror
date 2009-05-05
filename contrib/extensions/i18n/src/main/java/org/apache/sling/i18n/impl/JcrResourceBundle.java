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

import javax.jcr.query.Query;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

public class JcrResourceBundle extends ResourceBundle {

    private static final String JCR_PATH = "jcr:path";

    private static final String PROP_KEY = "sling:key";

    private static final String PROP_VALUE = "sling:message";

    private static final String PROP_BASENAME = "sling:basename";

    private static final String QUERY_BASE = "//element(*,mix:language)[@jcr:language='%s'%s]/*";

    private static final String QUERY_LOAD_FULLY = QUERY_BASE + "/(@"
        + PROP_KEY + "|@" + PROP_VALUE + ")";

    private static final String QUERY_LOAD_RESOURCE = QUERY_BASE + "[@"
        + PROP_KEY + "='%s']/@" + PROP_VALUE;

    private final ResourceResolver resourceResolver;

    private Map<String, Object> resources;

    private boolean fullyLoaded;

    private final Locale locale;

    private final String baseName;

    private String[] searchPath;

    JcrResourceBundle(Locale locale, String baseName,
            ResourceResolver resourceResolver) {
        this.locale = locale;
        this.baseName = baseName;
        this.resourceResolver = resourceResolver;
        this.resources = new HashMap<String, Object>();
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

            Iterator<Map<String, Object>> bundles = resourceResolver.queryResources(
                getFullLoadQuery(), Query.XPATH);

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

    private Object loadResource(String key) {
        // query for the resource
        Iterator<Map<String, Object>> bundles = resourceResolver.queryResources(
            getResourceQuery(key), Query.XPATH);
        if (bundles.hasNext()) {

            String[] path = getSearchPath();

            Map<String, Object> currentValue = null;
            int currentWeight = path.length;

            while (bundles.hasNext() && currentWeight > 0) {
                Map<String, Object> resource = bundles.next();
                String jcrPath = (String) resource.get(JCR_PATH);

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

            return currentValue.get(PROP_VALUE);
        }

        return null;
    }

    private String getFullLoadQuery() {
        return String.format(QUERY_LOAD_FULLY, getLocale(), getBaseNameTerm());
    }

    private String getResourceQuery(String key) {
        return String.format(QUERY_LOAD_RESOURCE, getLocale(),
            getBaseNameTerm(), key);
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
