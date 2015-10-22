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
package org.apache.sling.distribution.transport.impl;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionException;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.transport.DistributionTransportSecret;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;

/**
 * Advanced HTTP {@link org.apache.sling.distribution.transport.core.DistributionTransport} supporting custom HTTP headers
 * and body.
 */
public class AdvancedHttpDistributionTransport extends SimpleHttpDistributionTransport {

    private static final String PATH_VARIABLE_NAME = "{path}";


    private final DistributionEndpoint distributionEndpoint;

    private final boolean useCustomHeaders;

    private final String[] customHeaders;

    private final boolean useCustomBody;

    private final String customBody;

    public AdvancedHttpDistributionTransport(DefaultDistributionLog log, boolean useCustomHeaders,
                                             String[] customHeaders,
                                             boolean useCustomBody,
                                             String customBody,
                                             DistributionEndpoint distributionEndpoint,
                                             DistributionPackageBuilder packageBuilder,
                                             DistributionTransportSecretProvider secretProvider,
                                             int maxNoOfPackages) {


        super(log, distributionEndpoint, packageBuilder, secretProvider, maxNoOfPackages);
        this.useCustomHeaders = useCustomHeaders;
        this.customHeaders = customHeaders;
        this.useCustomBody = useCustomBody;
        this.customBody = customBody;

        this.distributionEndpoint = distributionEndpoint;
    }

    @Override
    public void deliverPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionException {
        log.info("delivering package {} to {} using auth {}",
                distributionPackage.getId(),
                distributionEndpoint.getUri(), secretProvider);

        try {
            DistributionTransportSecret secret = secretProvider.getSecret(distributionEndpoint.getUri());
            Executor executor = authenticate(secret, Executor.newInstance());

            deliverPackage(executor, distributionPackage, distributionEndpoint);

        } catch (IOException ex) {
            throw new DistributionException(ex);
        }

    }

    public static String[] getCustomizedHeaders(String[] additionalHeaders, String action, String[] paths) {
        List<String> headers = new ArrayList<String>();

        for (String additionalHeader : additionalHeaders) {
            int idx = additionalHeader.indexOf("->");

            if (idx < 0) {
                headers.add(additionalHeader);
            } else {
                String actionSelector = additionalHeader.substring(0, idx).trim();
                String header = additionalHeader.substring(idx + 2).trim();

                if (actionSelector.equalsIgnoreCase(action) || actionSelector.equals("*")) {
                    headers.add(header);
                }
            }
        }

        StringBuilder sb = new StringBuilder();

        if (paths != null && paths.length > 0) {
            sb.append(paths[0]);
            for (int i = 1; i < paths.length; i++) {
                sb.append(", ").append(paths[i]);
            }
        }

        String path = sb.toString();

        List<String> boundHeaders = new ArrayList<String>();

        for (String header : headers) {
            boundHeaders.add(header.replace(PATH_VARIABLE_NAME, path));
        }

        return boundHeaders.toArray(new String[boundHeaders.size()]);
    }

    private void deliverPackage(Executor executor, DistributionPackage distributionPackage,
                                DistributionEndpoint distributionEndpoint) throws IOException {
        String type = distributionPackage.getType();

        Request req = Request.Post(distributionEndpoint.getUri()).useExpectContinue();

        if (useCustomHeaders) {
            String[] customizedHeaders = getCustomizedHeaders(customHeaders, distributionPackage.getInfo().getRequestType().name(),
                    distributionPackage.getInfo().getPaths());
            for (String header : customizedHeaders) {
                addHeader(req, header);
            }
        }

        InputStream inputStream = null;
        Response response = null;
        try {
            if (useCustomBody) {
                String body = customBody == null ? "" : customBody;
                inputStream = new ByteArrayInputStream(body.getBytes("UTF-8"));
            } else {
                inputStream = distributionPackage.createInputStream();
            }

            req = req.bodyStream(inputStream, ContentType.APPLICATION_OCTET_STREAM);

            response = executor.execute(req);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        Content content = response.returnContent();
        log.info("Distribution content of type {} for {} delivered: {}", new Object[]{
                type, Arrays.toString(distributionPackage.getInfo().getPaths()), content});
    }

    private static void addHeader(Request req, String header) {
        int idx = header.indexOf(":");
        if (idx < 0) return;
        String headerName = header.substring(0, idx).trim();
        String headerValue = header.substring(idx + 1).trim();
        req.addHeader(headerName, headerValue);
    }
}