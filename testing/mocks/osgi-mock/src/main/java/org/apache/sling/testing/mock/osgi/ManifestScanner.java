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
package org.apache.sling.testing.mock.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scan METAINF/MANIFEST.MF files.
 */
public final class ManifestScanner {
    
    private static final Logger log = LoggerFactory.getLogger(ManifestScanner.class);
    
    private ManifestScanner() {
        // static methods only
    }

    /**
     * Get all bundle header values stored in MANIFEST.MF files as attributes.
     * Attributes values from all manifest files are collected, and values separated by "," are returned individually.
     * The order of the values from each entry is preserved, but the overall order when multiple bundles define such an entry
     * is not deterministic. Duplicate values are eliminated.
     * @param attributeName Attribute / Bundle header name.
     * @return List of values.
     */
    public static Collection<String> getValues(final String attributeName) {
        Set<String> values = new LinkedHashSet<String>();
        try {
            Enumeration<URL> resEnum = ManifestScanner.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (resEnum.hasMoreElements()) {
                try {
                    URL url = (URL)resEnum.nextElement();
                    InputStream is = url.openStream();
                    if (is != null) {
                        try {
                            Manifest manifest = new Manifest(is);
                            Attributes mainAttribs = manifest.getMainAttributes();
                            String valueList = mainAttribs.getValue(attributeName);
                            String[] valueArray = StringUtils.split(valueList, ",");
                            if (valueArray != null) {
                                for (String value : valueArray) {
                                    if (!StringUtils.isBlank(value)) {
                                        values.add(StringUtils.trim(value));
                                    }
                                }
                            }
                        }
                        finally {
                            is.close();
                        }
                    }
                }
                catch (Throwable ex) {
                    log.warn("Unable to read JAR manifest.", ex);
                }
            }
        }
        catch (IOException ex) {
            log.warn("Unable to read JAR manifests.", ex);
        }
        return values; 
    }

}
