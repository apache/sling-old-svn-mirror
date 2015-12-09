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
package org.apache.sling.distribution.serialization;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.distribution.DistributionRequestType;

/**
 * Additional information about a package.
 * Additional information is optional and components should expect every piece of it to be null.
 */
@ProviderType
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
     * distribution request deep paths
     */
    public static String PROPERTY_REQUEST_DEEP_PATHS = "request.deepPaths";


    /**
     * distribution request type
     */
    public static String PROPERTY_REQUEST_TYPE = "request.type";


    /**
     * Creates a new wrapper around a given map.
     *
     * @param base wrapped object
     */
    public DistributionPackageInfo(String packageType, Map<String, Object> base) {
        super(base);
        if (packageType == null) {
            throw new IllegalArgumentException("package type cannot be null");
        }

        put(PROPERTY_PACKAGE_TYPE, packageType);
    }


    /**
     * Creates a new wrapper around an empty map.
     *
     */
    public DistributionPackageInfo(String packageType) {
        this(packageType, new HashMap<String, Object>());
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

    @Override
    public String toString() {
        return "DistributionPackageInfo{" +
                " request.type=" + get(PROPERTY_REQUEST_TYPE, DistributionRequestType.class) +
                ", request.paths=" + Arrays.toString(get(PROPERTY_REQUEST_PATHS, String[].class)) +
                '}';
    }
}
