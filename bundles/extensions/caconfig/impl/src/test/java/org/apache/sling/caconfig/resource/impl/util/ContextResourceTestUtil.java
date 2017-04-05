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

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.spi.ContextResource;

public final class ContextResourceTestUtil {
    
    private ContextResourceTestUtil() {
        // static methods only
    }
    
    @SuppressWarnings("unchecked")
    public static Iterator<Resource> toResourceIterator(Iterator<ContextResource> contextResources) {
        return IteratorUtils.transformedIterator(contextResources, new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((ContextResource)input).getResource();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static Iterator<String> toConfigRefIterator(Iterator<ContextResource> contextResources) {
        return IteratorUtils.transformedIterator(contextResources, new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((ContextResource)input).getConfigRef();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static Iterator<ContextResource> toContextResourceIterator(Iterator<Resource> resources) {
        return IteratorUtils.transformedIterator(resources, new Transformer() {
            @Override
            public Object transform(Object input) {
                Resource resource = (Resource)input;
                return new ContextResource(resource, "/conf-test" + resource.getPath());
            }
        });
    }

}
