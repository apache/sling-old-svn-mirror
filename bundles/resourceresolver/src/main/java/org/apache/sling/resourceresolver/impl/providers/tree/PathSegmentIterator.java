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
package org.apache.sling.resourceresolver.impl.providers.tree;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PathSegmentIterator implements Iterator<String> {

    private final String path;

    private int index = 0;

    public PathSegmentIterator(String path, int index) {
        this.path = path;
        this.index = index;
    }

    @Override
    public boolean hasNext() {
        return index < path.length();
    }

    @Override
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        int nextIndex = path.indexOf('/', index);
        if (nextIndex == -1) {
            nextIndex = path.length();
        }
        String result = path.substring(index, nextIndex);
        index = nextIndex + 1;
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
