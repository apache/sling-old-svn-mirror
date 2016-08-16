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

import org.osgi.annotation.versioning.ProviderType;

/**
 * The dynamic class loader manager is a central
 * service managing all dynamic class loaders.
 * It provides a class loader that can be used by
 * bundles requiring access to all publically available
 * classes.
 *
 * The default implementation uses the package admin
 * service to load classes and resources.
 *
 * Keep in mind, that the class loader might get invalid.
 * This happens for example, if the class loader loaded
 * a class from a bundle which has been updated in the
 * meantime. Or a dynamic class loader provider has changed.
 *
 * In these cases, the dynamic class loader manager service
 * is unregistered and reregistered again, so you should
 * discard your classloader and invalidate loaded objects
 * whenever this happens.
 */
@ProviderType
public interface DynamicClassLoaderManager {

    /**
     * The dynamic class loader.
     * @return The dynamic class loader.
     */
    ClassLoader getDynamicClassLoader();
}
