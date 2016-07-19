/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.auth.requirement.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nonnull;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private Utils() {}

    /**
     * Creates a new array from the given {@code strings} eliminating duplicates
     * and any path that doesn't denote a valid absolute path.
     *
     * @param strings The strings to be verified.
     * @return An array of valid absolute paths without any duplicates
     */
    static String[] getValidPaths(@Nonnull String[] strings) {
        Set<String> paths = Sets.newLinkedHashSetWithExpectedSize(strings.length);
        for (String s : strings) {
            if (!paths.add(s)) {
                log.debug("Duplicate entry '{}'", s);
            }
        }

        Iterator<String> it = paths.iterator();
        while (it.hasNext()) {
            String path = it.next();
            if (Strings.isNullOrEmpty(path) || !PathUtils.isValid(path) || !PathUtils.isAbsolute(path)) {
                log.debug("Ignoring invalid path in 'supportedPaths' property: '{}'", path);
                it.remove();
            }
        }
        return paths.toArray(new String[paths.size()]);
    }

    static String getCommonAncestor(@Nonnull String[] paths) {
        switch (paths.length) {
            case 0:
                return PathUtils.ROOT_PATH;
            case 1:
                String path = paths[0];
                return (Strings.isNullOrEmpty(path)) ? PathUtils.ROOT_PATH : path;
            default:
                ArrayList<String[]> l = new ArrayList<String[]>(paths.length);
                String[] shortest = null;
                for (String sp : paths) {
                    String[] elements = Text.explode(sp, '/', false);
                    if (elements.length > 0) {
                        l.add(elements);
                        if (shortest == null || elements.length < shortest.length) {
                            shortest = elements;
                        }
                    }
                }
                String queryRoot = PathUtils.ROOT_PATH;
                if (shortest != null) {
                    for (int i = 0; i < shortest.length; i++) {
                        String segm = shortest[i];
                        for (String[] elements : l) {
                            if (!segm.equals(elements[i])) {
                                return queryRoot;
                            }
                        }
                        if (PathUtils.denotesRoot(queryRoot)) {
                            queryRoot = queryRoot + segm;
                        } else {
                            queryRoot = queryRoot + '/' + segm;
                        }
                    }
                }
                return queryRoot;
        }
    }
}