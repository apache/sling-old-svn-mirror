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
package org.apache.sling.distribution.packaging;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.distribution.DistributionRequestType;

/**
 * Additional information about a package.
 * Additional information is optional and components should expect every piece of it to be null.
 */
public final class DistributionPackageInfo extends ValueMapDecorator implements ValueMap {


    /**
     * distribution package type
     */
    public static String PROPERTY_PACKAGE_TYPE = "package.type";

    /**
     * distribution request paths
     */
    public static String PROPERTY_REQUEST_PATHS = "request.paths";

    /**
     * distribution request type
     */
    public static String PROPERTY_REQUEST_TYPE = "request.type";

    /**
     * distribution package origin uri
     */
    public static String PROPERTY_ORIGIN_URI = "package.origin.uri";

    /**
     * distribution package origin queue
     */
    public static String PROPERTY_ORIGIN_QUEUE = "origin.queue";


    /**
     * Creates a new wrapper around a given map.
     *
     * @param base wrapped object
     */
    public DistributionPackageInfo(Map<String, Object> base) {
        super(init(null, base));
    }

    /**
     * Creates a new wrapper around a given map.
     *
     */
    public DistributionPackageInfo(String type) {
        super(init(type, null));
    }


    private static Map<String, Object> init(String type, Map<String, Object> base) {
        Map<String, Object> result = new HashMap<String, Object>();

        if (base != null) {
            type = (String) base.get(PROPERTY_PACKAGE_TYPE);

            result = new HashMap<String, Object>(base);
        }

        result.put(PROPERTY_PACKAGE_TYPE, type);

        return result;
    }

    @Nonnull
    public String getType() {
        return get(PROPERTY_PACKAGE_TYPE, String.class);
    }

    /**
     * get the paths covered by the package holding this info
     *
     * @return an array of paths
     */
    @CheckForNull
    public String[] getPaths() {
        return get(PROPERTY_REQUEST_PATHS, String[].class);
    }

    /**
     * get the request type associated to the package holding this info
     *
     * @return the request type
     */
    @CheckForNull
    public DistributionRequestType getRequestType() {
        return get(PROPERTY_REQUEST_TYPE, DistributionRequestType.class);
    }

    /**
     * retrieves the origin of the package holding this info
     *
     * @return the package origin
     */
    @CheckForNull
    public URI getOrigin() {
        return get(PROPERTY_ORIGIN_URI, URI.class);
    }

    @CheckForNull
    public String getQueue() {
        return get(PROPERTY_ORIGIN_QUEUE, String.class);
    }


    @Override
    public String toString() {
        return "DistributionPackageInfo{" +
                " requestType=" + getRequestType() +
                ", paths=" + Arrays.toString(getPaths()) +
                ", origin=" + getOrigin() +
                '}';
    }
}
