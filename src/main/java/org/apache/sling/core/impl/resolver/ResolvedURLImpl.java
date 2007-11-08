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
package org.apache.sling.core.impl.resolver;

import org.apache.sling.api.request.RequestPathInfo;

/**
 * The <code>ResolvedURLImpl</code> class represents the URL after resolution
 * to the Content. It also contains the selectors and extension extracted from
 * the request URL.
 */
public class ResolvedURLImpl implements RequestPathInfo {

    /**
     * The original URL from which was decomposed into this instance. This field
     * must be set by either the constructor or later calling the
     * {@link #setOriginalURL(String)} method.
     */
    private String originalURL;

    /**
     * The decomposed array of the request selectors. By default the selectors
     * array is empty.
     */
    private String[] selectors = new String[0];

    /**
     * The complete selector string, elements separated by dots. By default the
     * selector string is empty.
     */
    private String selectorString = "";

    /**
     * The extension of the request URL. By default the extension is empty.
     */
    private String extension = "";

    /**
     * The suffix of the request URL. By default the suffix is empty.
     */
    private String suffix = "";

    /**
     * Creates an instance of this class for the original URL and the mapped
     * content.
     *
     * @param originalURL The original, unmodified URL. The content path need
     *      not necessairily be the prefix of this URL.
     * @param content The Content object resolved from the given original URL
     */
    public ResolvedURLImpl(String originalURL) {
        setOriginalURL(originalURL);
    }

    /**
     * Creates a new instance of this class copying the contents of the given
     * ResolvedURL into the new instance.
     *
     * @param resolvedURL The ResolvedURL to copy into the new instance
     *
     * @throws NullPointerException If resolvedURL is <code>null</code>.
     */
    public ResolvedURLImpl(RequestPathInfo resolvedURL) {
        setOriginalURL(resolvedURL.getResourcePath());
        setSelectorString(resolvedURL.getSelectorString());
        setExtension(resolvedURL.getExtension());
        setSuffix(resolvedURL.getSuffix());
    }

    //---------- ResolvedURL interface ----------------------------------------

    public String getResourcePath() {
        return originalURL;
    }

    public String[] getSelectors() {
        return selectors;
    }

    public String getSelectorString() {
        return selectorString;
    }

    public String getExtension() {
        return extension;
    }

    public String getSuffix() {
        return suffix;
    }

    //---------- setters for the parts of the URL decomposition ---------------

    public void setOriginalURL(String originalURL) {
        this.originalURL = originalURL;
    }

    /**
     * The selectors to set here is the string of dot-separated words after the
     * path but before any optional subsequent slashes. This string includes the
     * request URL extension, which is extracted here, too.
     *
     * @param The string of selectors and the extension to be decomposed into
     *      the selectors and the extension.
     *
     * @throws NullPointerException if selectors is <code>null</code>.
     */
    public void setSelectorsExtension(String selectors) {
        int lastDot = selectors.lastIndexOf('.');
        if (lastDot < 0) {
            // no selectors, just the extension
            setExtension(selectors);
            setSelectorString("");
            return;
        }

        // extension comes after last dot, rest are selectors
        setExtension(selectors.substring(lastDot + 1));
        setSelectorString(selectors.substring(0, lastDot));
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * Sets the selector string and the decomposed array of selectors.
     *
     * @throws NullPointerException If selectorString is <code>null</code>.
     */
    public void setSelectorString(String selectorString) {
        // cut off extension to split selectors
        this.selectorString = selectorString;
        this.selectors = selectorString.split("\\.");
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
