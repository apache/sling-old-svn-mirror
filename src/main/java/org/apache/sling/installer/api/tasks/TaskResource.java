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

import org.osgi.framework.Version;


/**
 * A task resource is a registered resource which has been
 * processed by a {@link ResourceTransformer} and is now
 * about to be processed by an {@link InstallTask}.
 */
public interface TaskResource extends RegisteredResource {

    /** Additional installation information in human readable format. */
    String ATTR_INSTALL_INFO = "org.apache.sling.installer.api.resource.install.info";

    /** If this attribute is set and the resource has the state installed,
     * it actually means that this resource has been processed but not installed.
     * For example this can be used to exclude environment specific bundles on non
     * supported environments etc.
     * The value of this attribute should contain some human readable reason why this
     * resource has been excluded.
     */
    String ATTR_INSTALL_EXCLUDED = "org.apache.sling.installer.api.resource.install.excluded";

    /**
     * Get the value of an attribute.
     * Attributes are specific to the resource and are either set
     * by a {@link ResourceTransformer} or a {@link InstallTask} for
     * processing.
     * Typical attributes are the bundle symbolic name or bundle version.
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

    /**
     * Return the version of the artifact.
     * @return The version of the artifact or <code>null</code>
     * @since 1.2
     */
    Version getVersion();
}
