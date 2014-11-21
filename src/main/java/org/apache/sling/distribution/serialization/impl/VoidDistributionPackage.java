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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.distribution.communication.DistributionActionType;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;

/**
 * A void {@link org.apache.sling.distribution.packaging.DistributionPackage} is used for deletion of certain paths on the target instance
 */
public class VoidDistributionPackage extends AbstractDistributionPackage implements DistributionPackage {

    private static final String TYPE = "VOID";

    private final String type;

    private final String[] paths;

    private final String id;

    private final String action;


    public VoidDistributionPackage(DistributionRequest request) {
        this(request, TYPE);
    }

    public VoidDistributionPackage(DistributionRequest request, String type) {
        this.type = type;
        this.paths = request.getPaths();
        this.action = request.getActionType().toString();
        this.id = request.getActionType().toString()
                + ':' + Arrays.toString(request.getPaths()).replaceAll("\\[", "").replaceAll("\\]", "")
                + ':' + request.getTime()
                + ':' + type;
    }

    public static VoidDistributionPackage fromStream(InputStream stream) throws IOException {
        String streamString = IOUtils.toString(stream);

        String[] parts = streamString.split(":");

        if (parts.length < 4) return null;

        String actionString = parts[0];
        String pathsString = parts[1];
        String timeString = parts[2];
        String typeString = parts[3];

        DistributionActionType distributionActionType = DistributionActionType.fromName(actionString);

        VoidDistributionPackage distributionPackage = null;
        if (distributionActionType != null) {
            pathsString = Text.unescape(pathsString);
            String[] paths = pathsString.split(", ");

            DistributionRequest request = new DistributionRequest(distributionActionType, paths);
            distributionPackage = new VoidDistributionPackage(request, typeString);
        }

        return distributionPackage;
    }


    private static final long serialVersionUID = 1L;

    @Nonnull
    public String getType() {
        return type;
    }

    @Nonnull
    public String[] getPaths() {
        return paths;
    }

    public long getLength() {
        try {
            return id.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported UTF-8 encoding");
        }
    }

    @Nonnull
    public InputStream createInputStream() throws IOException {
        return new ByteArrayInputStream(id.getBytes("UTF-8"));
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getActionType() {
        return action;
    }


    public void delete() {

    }


    @Override
    public String toString() {
        return "VoidDistributionPackage{" +
                "type='" + type + '\'' +
                ", paths=" + Arrays.toString(paths) +
                ", id='" + id + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}
