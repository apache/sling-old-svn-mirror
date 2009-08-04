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

/**
 * The class loader writer allows to modify the resources loaded by a
 * {@link DynamicClassLoaderProvider}. For example a class loader writer
 * could write generated class files into the repository or the temporary
 * file system.
 */
public interface ClassLoaderWriter {

    /**
     * Get the output stream for a class or resource handled
     * by the underlying class loader.
     * If the resource/class does not exists it should be created.
     * @param path The path of the class/resource.
     * @return The output stream.
     */
    OutputStream getOutputStream(String path);

    /**
     * Get the input stream for a class or resource handled
     * by the underlying class loader.
     * @param path The path of the class/resource.
     * @return The input stream for the resource/class.
     * @throws IOException If the resource/class does not exist.
     */
    InputStream getInputStream(String path) throws IOException;

    /**
     * Return the last modified for the class or resource.
     * @param path The path of the class/resource.
     * @return The last modified information or <code>-1</code> if
     *         the information can't be detected.
     */
    long getLastModified(String path);

    /**
     * Delete the class/resource
     * @param path The path of the class/resource.
     * @return <code>true</code> if the resource exists and could be deleted,
     *     <code>false</code> otherwise.
     */
    boolean delete(String path);

    /**
     * Rename a class/resource.
     * @param oldPath The path of the class/resource.
     * @param newPath The new path.
     * @return <code>true</code> if the renaming has been successful.
     */
    boolean rename(String oldPath, String newPath);
}
