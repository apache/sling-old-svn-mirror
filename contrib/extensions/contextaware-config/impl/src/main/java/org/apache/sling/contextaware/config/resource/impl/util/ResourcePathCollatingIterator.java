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
package org.apache.sling.contextaware.config.resource.impl.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.iterators.CollatingIterator;
import org.apache.sling.api.resource.Resource;

/**
 * Expected a list of iterators containing paths, where each path is a direct or indirect parent of the previous one
 * (= sorted by path hierarchy starting with the deepest path).
 * Result is a new iterator with all resources combined from all iterators in the same order, duplicates not eliminated. 
 */
public class ResourcePathCollatingIterator extends CollatingIterator {

    private static Comparator<Resource> PATH_LENGTH_COMPARATOR = new Comparator<Resource>() {
        @Override
        public int compare(Resource o1, Resource o2) {
            Integer length1 = o1.getPath().length();
            Integer length2 = o2.getPath().length();
            return length2.compareTo(length1);
        }
    };

    public ResourcePathCollatingIterator(List<Iterator<Resource>> iterator) {
        super(PATH_LENGTH_COMPARATOR, iterator);
    }
    
}
