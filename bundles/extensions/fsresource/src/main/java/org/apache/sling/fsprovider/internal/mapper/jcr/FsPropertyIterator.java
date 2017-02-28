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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.fsprovider.internal.mapper.ContentFile;

/**
 * Simplified implementation of read-only content access via the JCR API.
 */
class FsPropertyIterator implements PropertyIterator {
    
    private final Iterator<String> propertyNames;
    private final ContentFile contentFile;
    private final ResourceResolver resolver;
    private final Node node;
    
    public FsPropertyIterator(Iterator<String> propertyNames, ContentFile contentFile, ResourceResolver resolver, Node node) {
        this.propertyNames = propertyNames;
        this.contentFile = contentFile;
        this.resolver = resolver;
        this.node = node;
    }

    public boolean hasNext() {
        return propertyNames.hasNext();
    }

    public Object next() {
        return nextProperty();
    }

    @Override
    public Property nextProperty() {
        return new FsProperty(contentFile, resolver, propertyNames.next(), node);
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
