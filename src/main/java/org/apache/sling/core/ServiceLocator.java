/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core;

import org.osgi.framework.InvalidSyntaxException;

/**
 * <code>ServiceLocator</code>... The service locator is the gateway to all
 * registered components. It is made available through a request attribute.
 */
public interface ServiceLocator {

    /** The name of the request attribute holding the service locator. */
    String REQUEST_ATTRIBUTE_NAME = "org.apache.sling.core.ServiceLocator";

    /**
     * Lookup and return the service from the service registry.
     * 
     * @param serviceName The name (interface) of the service.
     * @return The service object or null if the service is not available.
     */
    Object getService(String serviceName);

    /**
     * Lookup and return the services from the service registry.
     * 
     * @param serviceName The name (interface) of the service.
     * @param filter An optional filter
     * @return The services object or null.
     */
    Object[] getService(String serviceName, String filter)
            throws InvalidSyntaxException;
}
