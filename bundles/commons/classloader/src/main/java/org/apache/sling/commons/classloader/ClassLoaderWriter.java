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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.osgi.annotation.versioning.ProviderType;


/**
 * A class loader writer is a service allowing to dynamically generate
 * classes and resources.
 * It provides methods for writing, removing, and moving resources.
 * In addition it provides a dynamic class loader which can be used
 * to dynamically load generated classes.
 * For example a class loader writer could write generated class files
 * into the repository or the temporary file system.
 */
@ProviderType
public interface ClassLoaderWriter {

    /**
     * Get the output stream for a class or resource handled
     * by the underlying class loader.
     * If the resource/class does not exists it should be created.
     * @param path The path of the class/resource. The path should be
     *             absolute like /com/my/domain/HelloWorld.class
     * @return The output stream.
     */
    OutputStream getOutputStream(String path);

    /**
     * Get the input stream for a class or resource handled
     * by the underlying class loader.
     * @param path The path of the class/resource. The path should be
     *             absolute like /com/my/domain/HelloWorld.class
     * @return The input stream for the resource/class.
     * @throws IOException If the resource/class does not exist.
     */
    InputStream getInputStream(String path) throws IOException;

    /**
     * Return the last modified for the class or resource.
     * @param path The path of the class/resource. The path should be
     *             absolute like /com/my/domain/HelloWorld.class
     * @return The last modified information or <code>-1</code> if
     *         the information can't be detected.
     */
    long getLastModified(String path);

    /**
     * Delete the class/resource
     * @param path The path of the class/resource. The path should be
     *             absolute like /com/my/domain/HelloWorld.class
     * @return <code>true</code> if the resource exists and could be deleted,
     *     <code>false</code> otherwise.
     */
    boolean delete(String path);

    /**
     * Rename a class/resource. The paths should be
     * absolute like /com/my/domain/HelloWorld.class
     * @param oldPath The path of the class/resource.
     * @param newPath The new path.
     * @return <code>true</code> if the renaming has been successful.
     */
    boolean rename(String oldPath, String newPath);

    /**
     * Get a dynamic class loader.
     * The returned class loader can be used to load classes and resources generated
     * through this class loader writer. The parent of this class loader is
     * a class loader from the {@link DynamicClassLoaderManager}.
     * The class loader returned by this method should not be cached, as it might
     * get stale (e.g. used classes are removed etc.). Therefore each time a newly
     * generated class is loaded, the class loader should be fetched again using
     * this method.
     * The implementation might cache the class loader and return the same loader
     * on subsequent calls for as long as possible.
     * Clients of the class loader can use the {@link DynamicClassLoader#isLive()}
     * method to check if the fetched instance can still be used.
     *
     * @return A dynamic class loader implementing {@link DynamicClassLoader}
     * @since 1.3
     */
    ClassLoader getClassLoader();
}
