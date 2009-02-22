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
package org.apache.sling.api.adapter;

/**
 * The <code>AdapterFactory</code> interface defines the API for helpers which
 * may be provided to enhance the adaptability of adaptable objects.
 * <p>
 * Implementations of this interface are registered as OSGi services and are
 * used by the {@link AdapterManager} to adapt objects on demand. The
 * <code>AdapterFactory</code> services are not really intended to be used by
 * clients directly.
 */
public interface AdapterFactory {

    /**
     * The service name to use when registering implementations of this
     * interface as services (value is
     * "org.apache.sling.osgi.commons.AdapterFactory").
     */
    static final String SERVICE_NAME = AdapterFactory.class.getName();

    /**
     * The service registration property listing the fully qualified names of
     * classes which can be adapted by this adapter factory (value is
     * "adaptables"). The "adaptable" parameters of the
     * {@link #getAdapter(Object, Class)} method must be an instance of any of
     * these classes for this factory to be able to adapt the object.
     */
    static final String ADAPTABLE_CLASSES = "adaptables";

    /**
     * The service registration property listing the fully qualified names of
     * classes to which this factory can adapt adaptables (value is "adapters").
     */
    static final String ADAPTER_CLASSES = "adapters";

    /**
     * Adapt the given object to the adaptable type. The adaptable object is
     * guaranteed to be an instance of one of the classes listed in the
     * {@link #ADAPTABLE_CLASSES} services registration property. The type
     * parameter is on of the classes listed in the {@link #ADAPTER_CLASSES}
     * service registration properties.
     * <p>
     * This method may return <code>null</code> if the adaptable object may
     * not be adapted to the adapter (target) type for any reason. In this case,
     * the implementation should log a message to the log facility noting the
     * cause for not being able to adapt.
     * <p>
     * Note that the <code>adaptable</code> object is not required to
     * implement the <code>Adaptable</code> interface, though most of the time
     * this method is called by means of calling the
     * {@link org.apache.sling.adapter.SlingAdaptable#adaptTo(Class)} method.
     *
     * @param <AdapterType> The generic type of the adapter (target) type.
     * @param adaptable The object to adapt to the adapter type.
     * @param type The type to which the object is to be adapted.
     * @return The adapted object or <code>null</code> if this factory
     *         instance cannot adapt the object.
     */
    <AdapterType> AdapterType getAdapter(Object adaptable,
            Class<AdapterType> type);

}
