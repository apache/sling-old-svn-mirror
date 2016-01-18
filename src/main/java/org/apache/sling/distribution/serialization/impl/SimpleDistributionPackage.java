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
package org.apache.sling.distribution.serialization.impl;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link DistributionPackage} is used for deletion of certain paths on the target instance
 */
public class SimpleDistributionPackage extends AbstractDistributionPackage implements DistributionPackage {

    private static final Logger log = LoggerFactory.getLogger(SimpleDistributionPackage.class);

    private final static String PACKAGE_START = "DSTRPCK:";
    private final static String DELIM = "|";
    private final static String PATH_DELIM = ",";
    private final long size;

    public SimpleDistributionPackage(DistributionRequest request, String type) {
        super(toIdString(request, type), type);
        String[] paths = request.getPaths();
        DistributionRequestType requestType = request.getRequestType();

        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, paths);
        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, requestType);
        this.size = getId().toCharArray().length;
    }

    private static String toIdString(DistributionRequest request, String type) {

        StringBuilder b = new StringBuilder();

        b.append(PACKAGE_START);

        b.append(request.getRequestType().toString());
        b.append(DELIM);

        String[] paths = request.getPaths();

        if (paths != null && paths.length != 0) {
            for (int i = 0; i < paths.length; i++) {
                b.append(paths[i]);
                if (i < paths.length - 1) {
                    b.append(PATH_DELIM);
                }
            }
        }

        return b.toString();
    }

    public static SimpleDistributionPackage fromIdString(String id, String type) {
        if (!id.startsWith(PACKAGE_START)) {
            return null;
        }

        id = id.substring(PACKAGE_START.length());

        String[] parts = id.split(Pattern.quote(DELIM));

        if (parts.length < 1 || parts.length > 2) {
            return null;
        }

        String actionString = parts[0];
        String pathsString = parts.length < 2 ? null : parts[1];


        DistributionRequestType distributionRequestType = DistributionRequestType.fromName(actionString);

        SimpleDistributionPackage distributionPackage = null;
        if (distributionRequestType != null) {
            String[] paths = pathsString == null ? null : pathsString.split(PATH_DELIM);

            DistributionRequest request = new SimpleDistributionRequest(distributionRequestType, paths);
            distributionPackage = new SimpleDistributionPackage(request, type);
        }

        return distributionPackage;
    }


    @Nonnull
    public InputStream createInputStream() throws IOException {
        return IOUtils.toInputStream(getId(), "UTF-8");
    }

    @Override
    public long getSize() {
        return size;
    }


    public void close() {
        // there's nothing to close
    }

    public void delete() {
        // there's nothing to delete
    }

    @Override
    public String toString() {
        return getId();
    }

    public static SimpleDistributionPackage fromStream(InputStream stream, String type) {

        try {
            int size = SimpleDistributionPackage.PACKAGE_START.getBytes("UTF-8").length;
            stream.mark(size);
            byte[] buffer = new byte[size];
            int bytesRead = stream.read(buffer, 0, size);
            stream.reset();
            String s = new String(buffer, "UTF-8");

            if (bytesRead > 0 && buffer[0] > 0 && s.startsWith(SimpleDistributionPackage.PACKAGE_START)) {
                String streamString = IOUtils.toString(stream, "UTF-8");

                return fromIdString(streamString, type);
            }
        } catch (IOException e) {
            log.error("cannot read stream", e);
        }

        return null;
    }
}
