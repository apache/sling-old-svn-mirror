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
package org.apache.sling.installer.api.tasks;


/**
 * A task resource is a registered resource which has been
 * processed by a {@link ResourceTransformer} and is now
 * about to be processed by an {@InstallTask}.
 */
public interface TaskResource extends RegisteredResource {

    /**
     * Get the value of an attribute.
     * Attributes include the bundle symbolic name, bundle version, etc.
     * @param key The name of the attribute
     * @return The value of the attribute or <code>null</code>
     */
    Object getAttribute(String key);

    /**
     * Set the value of an attribute.
     * @param key The name of the attribute
     * @param value The attribute value or <code>null</code> to remove it.
     */
    void setAttribute(String key, Object value);

    /**
     * Get the current state of the resource.
     */
    ResourceState getState();

    /**
     * Get the value of a temporary attribute.
     * @param key The name of the attribute
     * @return The value of the attribute or <code>null</code>
     */
    Object getTemporaryAttribute(String key);

    /**
     * Set the value of a temporary attribute.
     * @param key The name of the attribute
     * @param value The attribute value or <code>null</code> to remove it.
     */
    void setTemporaryAttribute(String key, Object value);
}
