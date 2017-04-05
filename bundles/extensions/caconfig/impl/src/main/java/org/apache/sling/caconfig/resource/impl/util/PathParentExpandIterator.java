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
package org.apache.sling.caconfig.resource.impl.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceUtil;

/**
 * Expands all paths from the iterator with their parent paths up to the given root paths.
 * The expanded path are added directly after each given path.
 * Duplicates are not eliminated.
 */
public class PathParentExpandIterator implements Iterator<String> {
    
    private final String rootPath;
    private final Iterator<String> paths;
    private final Queue<String> expandedPaths = new LinkedList<>();
    
    public PathParentExpandIterator(String rootPath, Iterator<String> paths) {
        this.rootPath = rootPath;
        this.paths = paths;
    }

    @Override
    public boolean hasNext() {
        return paths.hasNext() || !expandedPaths.isEmpty();
    }

    @Override
    public String next() {
        if (expandedPaths.isEmpty()) {
            expandPaths(paths.next());
        }
        return expandedPaths.remove();
    }
    
    private void expandPaths(String path) {
        expandedPaths.add(path);
        String parentPath = ResourceUtil.getParent(path);
        if (parentPath != null && !StringUtils.equals(parentPath, rootPath)) {
            expandPaths(parentPath);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
