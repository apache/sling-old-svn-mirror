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
package org.apache.sling.fsprovider.internal.mapper.jcr;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.fsprovider.internal.mapper.ContentFile;

/**
 * Simplified implementation of read-only content access via the JCR API.
 */
class FsNodeIterator implements NodeIterator {
    
    private final ContentFile contentFile;
    private final ResourceResolver resolver;
    private final Iterator<Map.Entry<String,Map<String,Object>>> children;

    @SuppressWarnings("unchecked")
    public FsNodeIterator(ContentFile contentFile, ResourceResolver resolver) {
        this.contentFile = contentFile;
        this.resolver = resolver;
        Map<String,Object> content = (Map<String,Object>)contentFile.getContent();
        this.children = IteratorUtils.filteredIterator(content.entrySet().iterator(), new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                Map.Entry<String,Object> entry = (Map.Entry<String,Object>)object;
                return (entry.getValue() instanceof Map);
            }
        });
    }

    public boolean hasNext() {
        return children.hasNext();
    }

    public Object next() {
        return nextNode();
    }

    @Override
    public Node nextNode() {
        Map.Entry<String,Map<String,Object>> nextEntry = children.next();
        return new FsNode(contentFile.navigateToRelative(nextEntry.getKey()), resolver);
    }

    
    // --- unsupported methods ---
        
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void skip(long skipNum) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getPosition() {
        throw new UnsupportedOperationException();
    }

}
