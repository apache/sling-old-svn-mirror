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
package org.apache.sling.adapter.internal;

import java.util.TreeMap;

import org.osgi.framework.ServiceReference;

/**
 * The <code>AdapterFactoryDescriptorMap</code> is a sorted map of
 * {@link AdapterFactoryDescriptor} instances indexed (and ordered) by their
 * {@link ServiceReference}. This map is used to organize the
 * registered {@link org.apache.sling.api.adapter.AdapterFactory} services for
 * a given adaptable type.
 * <p>
 * Each entry in the map is a {@link AdapterFactoryDescriptor} thus enabling the
 * registration of multiple factories for the same (adaptable, adapter) type
 * tuple. Of course only the first entry (this is the reason for having a sorted
 * map) for such a given tuple is actually being used. If that first instance is
 * removed the eventual second instance may actually be used instead.
 */
public class AdapterFactoryDescriptorMap extends
        TreeMap<ServiceReference, AdapterFactoryDescriptor> {

    private static final long serialVersionUID = 2L;

}
