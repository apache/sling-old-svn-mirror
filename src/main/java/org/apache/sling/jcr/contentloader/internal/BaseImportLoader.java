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
package org.apache.sling.jcr.contentloader.internal;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.sling.jcr.contentloader.ContentReader;
import org.apache.sling.jcr.contentloader.ImportOptions;

/**
 * Base class that takes care of the details that are common to bundle content
 * loader and the POST operation "import" loader.
 */
public abstract class BaseImportLoader extends JcrXmlImporter {

    public static final String EXT_JCR_XML = ".jcr.xml";

    private ContentReaderWhiteboard contentReaderWhiteboard;

    // This constructor is meant to be used by the OSGi
    public BaseImportLoader() {
    }

    // This constructor is meant to be used by non-OSGi
    public BaseImportLoader(ContentReaderWhiteboard contentReaderWhiteboard) {
        this.contentReaderWhiteboard = contentReaderWhiteboard;
    }

    protected void bindContentReaderWhiteboard(final ContentReaderWhiteboard service) {
        this.contentReaderWhiteboard = service;
    }

    protected void unbindContentReaderWhiteboard(final ContentReaderWhiteboard service) {
        if ( this.contentReaderWhiteboard == service ) {
            this.contentReaderWhiteboard = null;
        }
    }

    public Map<String, ContentReader> getContentReaders() {
        Map<String, ContentReader> readers = new LinkedHashMap<String, ContentReader>();
        readers.put(EXT_JCR_XML, null);
        for (Entry<String, ContentReader> e : contentReaderWhiteboard.getReadersByExtension().entrySet()) {
            readers.put('.' + e.getKey(), e.getValue());
        }
        return readers;
    }

    protected String toPlainName(String name, String contentReaderExtension) {
        if (contentReaderExtension != null) {
            if (name.length() == contentReaderExtension.length()) {
                return null; // no name is provided
            }
            return name.substring(0, name.length() - contentReaderExtension.length());
        }
        return name;
    }

    /**
     * Get the extension of the file name.
     *
     * @param name The file name.
     * @return The extension a reader is registered for - or <code>null</code>
     */
    protected String getContentReaderExtension(String name) {
        String readerExt = null;
        final Iterator<String> ipIter = getContentReaders().keySet().iterator();
        while (readerExt == null && ipIter.hasNext()) {
            final String ext = ipIter.next();
            if (name.endsWith(ext)) {
                readerExt = ext;
            }
        }
        return readerExt;
    }

    /**
     * Return the content reader for the name
     *
     * @param name The file name.
     * @return The reader or <code>null</code>
     */
    public ContentReader getContentReader(String name, PathEntry configuration) {
        final Map<String, ContentReader> contentReaders = getContentReaders();
        for (Map.Entry<String, ContentReader> readerEntry : contentReaders.entrySet()) {
            String extension = readerEntry.getKey();
            if (name.endsWith(extension) && !configuration.isIgnoredImportProvider(extension)) {
                return readerEntry.getValue();
            }
        }
        return null;
    }

    public ContentReader getContentReader(String name, ImportOptions importOptions) {
        final Map<String, ContentReader> contentReaders = getContentReaders();
        for (Map.Entry<String, ContentReader> readerEntry : contentReaders.entrySet()) {
            String extension = readerEntry.getKey();
            if (name.endsWith(extension) && !importOptions.isIgnoredImportProvider(extension)) {
                return readerEntry.getValue();
            }
        }
        return null;
    }
}
