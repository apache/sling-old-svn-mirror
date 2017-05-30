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
package org.apache.sling.jcr.contentparser;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Content types.
 */
@ProviderType
public enum ContentType {

    /**
     * JSON content descriptor file.
     * @see <a href="https://sling.apache.org/documentation/bundles/content-loading-jcr-contentloader.html#json-descriptor-files">JCR ContentLoader JSON descriptor files</a>
     */
    JSON("json"),

    /**
     * XML content descriptor file.
     * @see <a href="https://sling.apache.org/documentation/bundles/content-loading-jcr-contentloader.html#xml-descriptor-files">JCR ContentLoader XML descriptor files</a>
     */
    XML("xml"),

    /**
     * JCR XML content (FileVault XML). Also known as extended document view XML.
     * Extends the regular document view as specified by JCR 2.0 by specifics like
     * multivalue and typing information. Is used by Jackrabbit FileVault.
     * @see <a href="https://docs.adobe.com/content/docs/en/spec/jcr/2.0/7_Export.html#7.3%20Document%20View">JCR 2.0, 7.3 Document View</a>
     * @see <a href="http://jackrabbit.apache.org/filevault/">Jackrabbit FileVault</a>
     */
    JCR_XML("jcr.xml");


    private final String extension;

    private ContentType(String extension) {
        this.extension = extension;
    }

    /**
     * @return Extension
     */
    public String getExtension() {
        return extension;
    }

}
