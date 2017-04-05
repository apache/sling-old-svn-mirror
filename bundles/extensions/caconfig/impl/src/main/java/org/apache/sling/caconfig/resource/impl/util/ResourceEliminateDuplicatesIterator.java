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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.caconfig.resource.spi.ContextResource;

/**
 * Iterator that eliminates duplicate resources (having same path).
 */
public class ResourceEliminateDuplicatesIterator extends FilterIterator {

    public ResourceEliminateDuplicatesIterator(Iterator<ContextResource> iterator) {
        super(iterator, new Predicate() {
            private final Set<String> keys = new HashSet<>();
            
            @Override
            public boolean evaluate(Object object) {
                ContextResource contextResource = (ContextResource)object;
                String key = contextResource.getResource().getPath() + "#" + StringUtils.defaultString(contextResource.getConfigRef());
                return keys.add(key);
            }
            
        });
    }
    
}
