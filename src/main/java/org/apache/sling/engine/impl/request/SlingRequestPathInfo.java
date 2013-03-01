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
package org.apache.sling.engine.impl.request;

import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;

/**
 * Sling request URI parser that provides SlingRequestPathInfo for the
 * current request, based on the path of the Resource. The values provided by
 * this depend on the Resource.getPath() value, as the ResourceResolver might
 * use all or only part of the request URI path to locate the resource (see also
 * SLING-60 ). What we're after is the remainder of the path, the part that was
 * not used to locate the Resource, and we split that part in different
 * subparts: selectors, extension and suffix.
 *
 * @see SlingRequestPathInfoTest for a number of examples.
 */
public class SlingRequestPathInfo implements RequestPathInfo {

    private final String selectorString;

    private final String[] selectors;

    private final String extension;

    private final String suffix;

    private final String resourcePath;

    private final Resource resource;

    private final static String[] NO_SELECTORS = new String[0];

    /** break requestPath as required by SlingRequestPathInfo */
    public SlingRequestPathInfo(Resource r) {

        // ensure the resource
        if (r == null) {
            throw new NullPointerException("resource");
        }

        this.resource = r;
        this.resourcePath = r.getResourceMetadata().getResolutionPath();

        // the extra path in the request URI
        String pathToParse = r.getResourceMetadata().getResolutionPathInfo();
        if (pathToParse == null) {
            pathToParse = "";
        }

        // separate selectors/ext from the suffix
        int firstSlash = pathToParse.indexOf('/');
        String pathToSplit;
        if (firstSlash < 0) {
            pathToSplit = pathToParse;
            suffix = null;
        } else {
            pathToSplit = pathToParse.substring(0, firstSlash);
            suffix = pathToParse.substring(firstSlash);
        }

        int lastDot = pathToSplit.lastIndexOf('.');

        if (lastDot <= 1) {

            // no selectors if only extension exists or selectors is empty
            selectorString = null;
            selectors = NO_SELECTORS;

        } else {

            // no selectors if splitting would give an empty array
            String tmpSel = pathToSplit.substring(1, lastDot);
            selectors = tmpSel.split("\\.");
            selectorString = (selectors.length > 0) ? tmpSel : null;

        }

        // extension only if lastDot is not trailing
        extension = (lastDot + 1 < pathToSplit.length())
                ? pathToSplit.substring(lastDot + 1)
                : null;
    }

    private SlingRequestPathInfo(Resource resource, String resourcePath, String selectorString, String extension,
            String suffix) {
        this.resource = resource;
        this.resourcePath = resourcePath;
        this.selectorString = selectorString;
        this.selectors = (selectorString != null)
                ? selectorString.split("\\.")
                : NO_SELECTORS;
        this.extension = extension;
        this.suffix = suffix;
    }

    public SlingRequestPathInfo merge(RequestPathInfo baseInfo) {
        if (getExtension() == null) {
            return new SlingRequestPathInfo(getResource(), getResourcePath(), baseInfo.getSelectorString(),
                baseInfo.getExtension(), baseInfo.getSuffix());
        }

        return this;
    }

    public SlingRequestPathInfo merge(RequestDispatcherOptions options) {

        if (options != null) {

            // set to true if any option is set
            boolean needCreate = false;

            // replacement selectors
            String selectors = options.getReplaceSelectors();
            if (selectors != null) {
                // if we get the empty string, all original selectors will be removed
                if ( selectors.length() == 0 ) {
                    selectors = null;
                }
                needCreate = true;
            } else {
                selectors = getSelectorString();
            }

            // additional selectors
            String selectorsAdd = options.getAddSelectors();
            if (selectorsAdd != null) {
                if (selectors != null) {
                    selectors += "." + selectorsAdd;
                } else {
                    selectors = selectorsAdd;
                }
                needCreate = true;
            }

            // suffix replacement
            String suffix = options.getReplaceSuffix();
            if (suffix != null) {
                needCreate = true;
            } else {
                suffix = getSuffix();
            }

            if (needCreate) {
                return new SlingRequestPathInfo(getResource(), getResourcePath(), selectors, getExtension(), suffix);
            }
        }

        return this;
    }

    @Override
    public String toString() {
        return "SlingRequestPathInfo: path='" + resourcePath + "'"
            + ", selectorString='" + selectorString + "'" + ", extension='"
            + extension + "'" + ", suffix='" + suffix + "'";
    }

    public String getExtension() {
        return extension;
    }

    public String[] getSelectors() {
        return selectors;
    }

    public String getSelectorString() {
        return selectorString;
    }

    public String getSuffix() {
        return suffix;
    }

    public Resource getSuffixResource() {
        if (this.suffix != null) {
            return getResource().getResourceResolver().getResource(this.suffix);
        }

        return null;
    }

    private Resource getResource() {
        return resource;
    }

    public String getResourcePath() {
        return resourcePath;
    }
}
