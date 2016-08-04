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
package org.apache.sling.installer.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * ResourceData just contains the data of a resource
 * - input stream
 * - dictionary
 *
 */
public class ResourceData {

    /**
     * Create an internal resource.
     * @throws IOException if something is wrong
     */
    public static ResourceData create(
            final InputStream stream,
            final Dictionary<String, Object> props)
    throws IOException {
        if ( stream == null ) {
            final Dictionary<String, Object> result = new Hashtable<String, Object>();
            final Enumeration<String> e = props.keys();
            while (e.hasMoreElements()) {
                final String key = e.nextElement();
                result.put(key, props.get(key));
            }
            return new ResourceData(result, null);

        }
        final File dataFile = FileDataStore.SHARED.createNewDataFile(stream,
                null, null, null);
        return new ResourceData(null, dataFile);
    }

    private final Dictionary<String, Object> dictionary;

    /** The data file (if copied) */
    private final File dataFile;

    private ResourceData(final Dictionary<String, Object> dict,
            final File dataFile) {
        this.dictionary = dict;
        this.dataFile = dataFile;
    }

    /**
     * Copy given Dictionary
     */
    public Dictionary<String, Object> getDictionary() {
        return this.dictionary;
    }

    /**
     * Return the file
     */
    public InputStream getInputStream() throws IOException {
        if ( this.dataFile != null ) {
            return new FileInputStream(this.dataFile);
        }
        return null;
    }

    public String getDigest(final String url, String digest) throws IOException {
        if ( this.dictionary != null ) {
            return digest != null ? digest : FileDataStore.computeDigest(this.dictionary);
        }
        if ( digest == null ) {
            digest = FileDataStore.computeDigest(this.dataFile);
        }
        FileDataStore.SHARED.updateDigestCache(url, this.dataFile, digest);
        return digest;

    }

    public File getDataFile() {
        return this.dataFile;
    }
}