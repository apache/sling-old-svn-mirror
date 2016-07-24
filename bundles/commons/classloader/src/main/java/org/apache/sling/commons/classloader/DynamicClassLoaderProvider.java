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
package org.apache.sling.commons.classloader;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A dynamic class loader provider allows to provide
 * class loaders that will be used by the dynamic
 * class loading mechanism. For instance a JCR class loader
 * provider could provide some class loader loading classes
 * from a content repository etc.
 *
 * @deprecated The dynamic class loader provider is not supported
 *             anymore and any service implementing this is not
 *             considered for dynamic class loading anymore!
 */
@Deprecated
@ConsumerType
public interface DynamicClassLoaderProvider {

    /**
     * Return the class loader used for dynamic class loading.
     * The returned class loader should use the provided parent class loader
     * as one of its parent class loaders. This ensures that the returned
     * class loader has access to all dynamically loaded classes that
     * are not part of this class loader.
     * When the class loader is not needed anymore, it is released by
     * calling the {@link #release(ClassLoader)} method.
     * @param parent The parent class loader for this dynamic class loader.
     * @return The class loader.
     * @see #release(ClassLoader)
     */
    ClassLoader getClassLoader(ClassLoader parent);

    /**
     * Release the provided class loader.
     * When the class loader is not needed anymore, e.g. when the dynamic class
     * loader is shutdown, it is released with this method.
     * The implementation can use this hook to free any allocated resources etc.
     * @param classLoader The class loader.
     * @see #getClassLoader(ClassLoader)
     * @since 2.0
     */
    void release(ClassLoader classLoader);
}
