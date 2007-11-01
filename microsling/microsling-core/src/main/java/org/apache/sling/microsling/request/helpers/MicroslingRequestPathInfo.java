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
package org.apache.sling.microsling.request.helpers;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

/**
 * microsling request URI parser that provides SlingRequestPathInfo for the
 * current request, based on the path of the Resource. The values provided by
 * this depend on the Resource.getURI() value, as the ResourceResolver might use
 * all or only part of the request URI path to locate the resource (see also
 * SLING-60 ). What we're after is the remainder of the path, the part that was
 * not used to locate the Resource, and we split that part in different
 * subparts: selectors, extension and suffix.
 *
 * @see MicroslingRequestPathInfoTest for a number of examples.
 */
public class MicroslingRequestPathInfo implements RequestPathInfo {

    private final String selectorString;

    private final String[] selectors;

    private final String extension;

    private final String suffix;

    private final String resourcePath;

    private final static String EMPTY = "";

    /** break requestPath as required by SlingRequestPathInfo */
    public MicroslingRequestPathInfo(Resource r, String requestPath) {

        // ensure the resource
        if (r == null) {
            throw new NullPointerException("resource");
        }

        String pathToParse = requestPath;
        if (pathToParse == null) {
            pathToParse = "";
        }

        resourcePath = (String) r.getResourceMetadata().get(
            ResourceMetadata.RESOLUTION_PATH);
        if (resourcePath != null && !"/".equals(resourcePath)
            && pathToParse.length() >= resourcePath.length()) {
            pathToParse = pathToParse.substring(resourcePath.length());
        }

        // separate selectors/ext from the suffix
        int firstSlash = pathToParse.indexOf('/');
        String pathToSplit;
        if (firstSlash < 0) {
            pathToSplit = pathToParse;
            suffix = EMPTY;
        } else {
            pathToSplit = pathToParse.substring(0, firstSlash);
            suffix = pathToParse.substring(firstSlash);
        }

        int lastDot = pathToSplit.lastIndexOf('.');
        if (lastDot <= 0) {
            // negative if pathToSplit is empty (in case all is suffix)
            // zero if pathToSplit just contains the extension
            selectorString = EMPTY;
            selectors = new String[0];
            extension = pathToSplit.substring(lastDot+1);
        } else {
            // assume the string to start with a dot
            selectorString = pathToSplit.substring(1, lastDot);
            selectors = selectorString.split("\\.");
            extension = pathToSplit.substring(lastDot + 1);
        }
    }

    @Override
    public String toString() {
        return "SlingRequestPathInfoParser:" + ", path='" + resourcePath + "'"
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

    public String getResourcePath() {
        return resourcePath;
    }
}
