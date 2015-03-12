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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.jcr.contentloader.internal.readers.JsonReader;
import org.apache.sling.jcr.contentloader.internal.readers.XmlReader;
import org.apache.sling.jcr.contentloader.internal.readers.ZipReader;

/**
 * Base class that takes care of the details that are common to bundle content
 * loader and the POST operation "import" loader.
 */
public abstract class BaseImportLoader extends JcrXmlImporter {
    public static final String EXT_XML = ".xml";
    public static final String EXT_JCR_XML = ".jcr.xml";
    public static final String EXT_JSON = ".json";
    public static final String EXT_JAR = ".jar";
    public static final String EXT_ZIP = ".zip";

    /** All available import providers. */
    Map<String, ImportProvider> defaultImportProviders;

	public BaseImportLoader() {
        defaultImportProviders = new LinkedHashMap<String, ImportProvider>();
        defaultImportProviders.put(EXT_JCR_XML, null);
        defaultImportProviders.put(EXT_JSON, JsonReader.PROVIDER);
        defaultImportProviders.put(EXT_XML, XmlReader.PROVIDER);
        defaultImportProviders.put(EXT_JAR, ZipReader.JAR_PROVIDER);
        defaultImportProviders.put(EXT_ZIP, ZipReader.ZIP_PROVIDER);
	}

    public void dispose() {
        defaultImportProviders = null;
    }

    protected String toPlainName(String name, String providerExtension) {
        if (providerExtension != null) {
            if (name.length() == providerExtension.length()) {
                return null; // no name is provided
            }
            return name.substring(0, name.length() - providerExtension.length());
        }
        return name;
    }

}
