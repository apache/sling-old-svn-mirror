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
package org.apache.sling.mongodb.impl;

import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;

public class ChangeableValueMap
    extends ReadableValueMap
    implements ModifiableValueMap {

    private final MongoDBResource resource;

    public ChangeableValueMap(final MongoDBResource resource) {
        super(resource.getProperties());
        this.resource = resource;
    }

    /**
     * @see java.util.Map#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(final String name, final Object value) {
        final Object oldValue = this.valueMap.get(name);
        final String key = MongoDBResourceProvider.propNameToKey(name);

        this.resource.getProperties().put(key, value);

        // update map and resource
        this.createValueMap(this.resource.getProperties());
        this.resource.changed();

        return oldValue;
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(final Map<? extends String, ? extends Object> m) {
        for(final Map.Entry<? extends String, ? extends Object> e : m.entrySet() ) {
            this.put(e.getKey(), e.getValue());
        }
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(final Object name) {
        final Object result = this.valueMap.get(name);
        if ( result != null ) {
            final String key = MongoDBResourceProvider.propNameToKey(name.toString());

            this.resource.getProperties().removeField(key);

            // update map and resource
            this.createValueMap(this.resource.getProperties());
            this.resource.changed();
        }
        return result;
    }
    
    protected MongoDBResource getResource() {
        return this.resource;
    }
}
