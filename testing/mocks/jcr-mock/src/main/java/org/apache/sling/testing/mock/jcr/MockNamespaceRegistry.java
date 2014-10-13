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
package org.apache.sling.testing.mock.jcr;

import java.util.Set;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Mock {@link NamespaceRegistry} implementation.
 */
class MockNamespaceRegistry implements NamespaceRegistry {

    private final BiMap<String, String> namespacePrefixMapping = HashBiMap.create();

    public MockNamespaceRegistry() {
        this.namespacePrefixMapping.put("jcr", "http://www.jcp.org/jcr/1.0");
    }

    @Override
    public String getURI(final String prefix) {
        return this.namespacePrefixMapping.get(prefix);
    }

    @Override
    public String getPrefix(final String uri) {
        return this.namespacePrefixMapping.inverse().get(uri);
    }

    @Override
    public void registerNamespace(final String prefix, final String uri) {
        this.namespacePrefixMapping.put(prefix, uri);
    }

    @Override
    public void unregisterNamespace(final String prefix) {
        this.namespacePrefixMapping.remove(prefix);
    }

    @Override
    public String[] getPrefixes() throws RepositoryException {
        Set<String> keys = this.namespacePrefixMapping.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    @Override
    public String[] getURIs() throws RepositoryException {
        Set<String> values = this.namespacePrefixMapping.values();
        return values.toArray(new String[values.size()]);
    }

}
