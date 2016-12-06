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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.iterators.CollatingIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.caconfig.resource.spi.ContextResource;

/**
 * Expected a list of iterators containing paths, where each path is a direct or indirect parent of the previous one
 * (= sorted by path hierarchy starting with the deepest path).
 * Result is a new iterator with all resources combined from all iterators in the same order, duplicates not eliminated. 
 */
public class ResourcePathCollatingIterator extends CollatingIterator {

    private static Comparator<ContextResource> PATH_LENGTH_COMPARATOR = new Comparator<ContextResource>() {
        @Override
        public int compare(ContextResource o1, ContextResource o2) {
            Integer length1 = o1.getResource().getPath().length();
            Integer length2 = o2.getResource().getPath().length();
            int result = length2.compareTo(length1);
            if (result == 0) {
                result = StringUtils.defaultString(o1.getConfigRef()).compareTo(StringUtils.defaultString(o2.getConfigRef()));
            }
            return result;
        }
    };

    public ResourcePathCollatingIterator(List<Iterator<ContextResource>> iterator) {
        super(PATH_LENGTH_COMPARATOR, iterator);
    }
    
}
