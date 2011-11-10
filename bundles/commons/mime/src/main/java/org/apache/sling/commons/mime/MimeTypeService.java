/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.mime;

import java.io.IOException;
import java.io.InputStream;

/**
 * The <code>MimeTypeService</code> defines the service applications can call to
 * resolve file names to MIME types and derive default file name extensions from
 * MIME types.
 * <p>
 * This interface is not intended to be implemented by bundles. It is
 * implemented by this bundle and may be used by client bundles.
 */
public interface MimeTypeService {

    /**
     * Returns the MIME type of the extension of the given <code>name</code>.
     * The extension is the part of the name after the last dot. If the name
     * does not contain a dot, the name as a whole is assumed to be the
     * extension.
     *
     * @param name The name for which the MIME type is to be returned.
     * @return The MIME type for the extension of the name. If the extension
     *         cannot be mapped to a MIME type or <code>name</code> is
     *         <code>null</code>, <code>null</code> is returned.
     * @see #getExtension(String)
     */
    String getMimeType(String name);

    /**
     * Returns the primary name extension to which the given
     * <code>mimeType</code> maps. The returned extension must map to the given
     * <code>mimeType</code> when fed to the {@link #getMimeType(String)}
     * method. In other words, the expression
     * <code>mimeType.equals(getMimeType(getExtension(mimeType)))</code> must
     * always be <code>true</code> for any non-<code>null</code> MIME type.
     * <p>
     * A MIME type may be mapped to multiple extensions (e.g.
     * <code>text/plain</code> to <code>txt</code>, <code>log</code>, ...). This
     * method is expected to returned one of those extensions. It is up to the
     * implementation to select an appropriate extension if multiple mappings
     * exist for a single MIME type.
     *
     * @param mimeType The MIME type whose primary extension is requested.
     * @return A extension which maps to the given MIME type or
     *         <code>null</code> if no such mapping exists.
     * @see #getMimeType(String)
     */
    String getExtension(String mimeType);

    /**
     * Dynamically register a new mime type with one or more file name
     * extensions. The first of those extensions is assumed to be default file
     * name extension.
     * <p>
     * This registration is dynamic and not persisted.
     *
     * @param mimeType The MIME type to register
     * @param extensions One or more file name extensions (without leading dot)
     *            to register for the MIME type.
     */
    void registerMimeType(String mimeType, String... extensions);

    /**
     * Register MIME types stored in the given input stream formatted as a
     * regular MIME type file format: One entry per line. Each entry consists of
     * two or more whitespace delimited fields where the first field is the MIME
     * type and the rest of the fields are the file extensions. The first of the
     * extensions is considered the default extension. Empty lines and lines
     * starting with a hash sign (<code>#</code>) are ignored.
     * <p>
     * The stream is assumed to contain string data encoded with "ISO-8859-1".
     * <p>
     * This method reads the stream until an <code>IOException</code> occurs or
     * until it has been fully read. The stream is not closed, though, by this
     * method.
     *
     * @param mimeTabStream The stream to read the MIME type mappings from
     * @throws IOException If an error occurs reading from the stream
     */
    void registerMimeType(InputStream mimeTabStream) throws IOException;
}
