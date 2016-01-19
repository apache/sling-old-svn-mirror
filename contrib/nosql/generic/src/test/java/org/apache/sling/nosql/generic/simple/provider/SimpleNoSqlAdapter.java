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
package org.apache.sling.nosql.generic.simple.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.nosql.generic.adapter.NoSqlAdapter;
import org.apache.sling.nosql.generic.adapter.NoSqlData;
import org.apache.sling.nosql.generic.resource.impl.PathUtil;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

/**
 * Stores resource data in a hash map for testing.
 */
public class SimpleNoSqlAdapter implements NoSqlAdapter {
    
    private final SortedMap<String, Map<String,Object>> store = new TreeMap<String, Map<String,Object>>();

    public boolean validPath(String path) {
        return !(StringUtils.contains(path, "/invalid/") || StringUtils.endsWith(path, "/invalid"));
    }

    public NoSqlData get(String path) {
        Map<String,Object> properties = store.get(path);
        if (properties != null) {
            return new NoSqlData(path, properties);
        }
        else {
            return null;
        }
    }

    public Iterator<NoSqlData> getChildren(String parentPath) {
        Iterator<String> keys = store.keySet().iterator();
        
        final Pattern childKeyPattern = PathUtil.getChildPathPattern(parentPath);
        Iterator<String> childKeys = Iterators.filter(keys, new Predicate<String>() {
            public boolean apply(String path) {
                return childKeyPattern.matcher(path).matches();
            }
        });
        
        return Iterators.transform(childKeys, new Function<String, NoSqlData>() {
            public NoSqlData apply(String path) {
                return get(path);
            }
        });
    }

    public boolean store(NoSqlData data) {
        boolean exists = store.containsKey(data.getPath());
        store.put(data.getPath(), new HashMap<String, Object>(data.getProperties()));
        return !exists;
    }

    public boolean deleteRecursive(String path) {
        boolean deletedAnything = false;
        final Pattern pathToDeletePattern = PathUtil.getSameOrDescendantPathPattern(path);
        Iterator<Entry<String, Map<String,Object>>> entries = store.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<String, Map<String,Object>> entry = entries.next();
            if (pathToDeletePattern.matcher(entry.getKey()).matches()) {
                entries.remove();
                deletedAnything = true;
            }
        }
        return deletedAnything;
    }

    public Iterator<NoSqlData> query(String query, String language) {
        // implement simple dummy query
        if (StringUtils.equals(language, "simple") && StringUtils.equals(query, "all")) {
            final Iterator<Entry<String, Map<String,Object>>> entries = store.entrySet().iterator();
            return new Iterator<NoSqlData>() {
                public boolean hasNext() {
                    return entries.hasNext();
                }
                public NoSqlData next() {
                    Entry<String, Map<String,Object>> entry = entries.next();
                    return new NoSqlData(entry.getKey(), entry.getValue());
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        return Collections.<NoSqlData>emptyList().iterator();
    }

    @Override
    public void checkConnection() throws LoginException {

    }

}
