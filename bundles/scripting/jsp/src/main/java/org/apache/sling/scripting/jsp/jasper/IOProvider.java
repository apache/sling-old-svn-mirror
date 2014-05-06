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
package org.apache.sling.scripting.jsp.jasper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.JavaCompiler;

/**
 * The <code>IOProvider</code> is an interface to provide more control of
 * sending output from JSP Java and Class generation phases as well as cleaning
 * up in case of problems.
 */
public interface IOProvider {

    /**
     * @param fileName The absolute path name of the destination into which to
     *      write the output. The semantics of this path depends on the
     *      implementation of this interface.
     *
     * @return an <code>OutputStream</code> into which to write the generated
     *      output.
     *
     * @throws IOException If the output stream cannot be created for the file.
     */
    OutputStream getOutputStream(String fileName) throws IOException;

    /**
     * Returns an input stream to a file which has been written through a
     * stream obtained form {@link #getOutputStream(String)}
     *
     * @param fileName The absolute path name of the source from which to
     *      read the input. The semantics of this path depends on the
     *      implementation of this interface.
     *
     * @return an <code>InputStream</code> from which to read the input.
     *
     * @throws FileNotFoundException If the file cannot be found
     * @throws IOException If any other error occurs.
     */
    InputStream getInputStream(String fileName)
            throws FileNotFoundException, IOException;

    /**
     * Remove a generated output.
     *
     * @param fileName The absolute path name of the item to remove. The
     *      semantics of this path depends on the implementation of this
     *      interface.
     *
     * @return <code>true</code> if the item could be removed, otherwise
     *      <code>false</code> is removed.
     */
    boolean delete(String fileName);

    /**
     * Renames the the file from the old file to the new file name
     * @param oldFileName
     * @param newFileName
     * @return <code>true</code> if renaming succeeded
     */
    boolean rename(String oldFileName, String newFileName);

    /**
     * Creates folders (folder like) structures, such that a container for data
     * is available at the given path.
     * <p>
     * In the case of a OS Filesystem implementation, this method would be
     * implemented by means of <code>new java.io.File(path).mkdirs()</code>.
     *
     * @param path The absolute path of the folder to create.
     *
     * @return <code>true</code> if and only if the folder was created, along
     *         with all necessary parent directories; <code>false</code>
     *         otherwise.
     */
    boolean mkdirs(String path);

    /**
     * Returns the last modification time stamp of the resource (generally a
     * file) at the given absolute location.
     *
     * @param fileName The absolute path to the file whose last modification
     *      time stamp is to be returned.
     *
     * @return The last modification time stamp of the resource in milliseconds
     *      since the epoch or -1 if no resource exists at the given location.
     */
    long lastModified(String fileName);

    /**
     * Return the class loader to use
     */
    ClassLoader getClassLoader();

    /**
     * Return the Java Compiler
     */
    JavaCompiler getJavaCompiler();

    /**
     * Return the class loader writer
     */
    ClassLoaderWriter getClassLoaderWriter();
}
