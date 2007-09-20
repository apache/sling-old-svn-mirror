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
package org.apache.sling.core.content;

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManagedHashMap;

/**
 * The <code>GenericContent</code> TODO
 *
 * @ocm.mapped jcrType="nt:unstructured" discriminator="true"
 */
public class GenericContent extends SelectableBaseContent {

    private ManagedHashMap properties;

    // get the named property from the component data
    public Object getProperty(String name) {
        return this.properties.get(name);
    }

    // store the named property and value to the component data
    public void setProperty(String name, Object value) {
        this.properties.put(name, value);
    }

    // ---- ObjectMapping support ----

    // sets the content data from the repository
    /**
     * @ocm.collection jcrName="*" elementClassName="java.lang.Object"
     *                 collectionConverter="org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ResidualPropertiesCollectionConverterImpl"
     */
    public void setProperties(ManagedHashMap contents) {
        // use a defensive copy of the map
        this.properties = contents;
    }

    // gets the content data to store into the repository
    public ManagedHashMap getProperties() {
        // use a defensive copy of the map
        return this.properties;
    }
}
