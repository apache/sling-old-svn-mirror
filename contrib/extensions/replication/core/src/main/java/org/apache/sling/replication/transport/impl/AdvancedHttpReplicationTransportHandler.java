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
package org.apache.sling.replication.transport.impl;

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
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced HTTP {@link org.apache.sling.replication.transport.ReplicationTransportHandler} supporting custom HTTP headers
 * and body.
 */
public class AdvancedHttpReplicationTransportHandler extends SimpleHttpReplicationTransportHandler {

    private static final String PATH_VARIABLE_NAME = "{path}";

    private static final Logger log = LoggerFactory.getLogger(AdvancedHttpReplicationTransportHandler.class);

    private final TransportAuthenticationProvider<Executor, Executor> transportAuthenticationProvider;
    private final ReplicationEndpoint replicationEndpoint;

    private final boolean useCustomHeaders;

    private final String[] customHeaders;

    private final boolean useCustomBody;

    private final String customBody;

    public AdvancedHttpReplicationTransportHandler(boolean useCustomHeaders,
                                                   String[] customHeaders,
                                                   boolean useCustomBody,
                                                   String customBody,
                                                   TransportAuthenticationProvider<Executor, Executor> transportAuthenticationProvider,
                                                   ReplicationEndpoint replicationEndpoint,
                                                   ReplicationPackageBuilder packageBuilder,
                                                   int maxNoOfPackages) {


        super(transportAuthenticationProvider, replicationEndpoint, packageBuilder, maxNoOfPackages);
        this.useCustomHeaders = useCustomHeaders;
        this.customHeaders = customHeaders;
        this.useCustomBody = useCustomBody;
        this.customBody = customBody;
        this.transportAuthenticationProvider = transportAuthenticationProvider;

        this.replicationEndpoint = replicationEndpoint;
    }

    @Override
    public void deliverPackage(ReplicationPackage replicationPackage) throws ReplicationTransportException {
        log.info("delivering package {} to {} using auth {}",
                new Object[]{replicationPackage.getId(),
                        replicationEndpoint.getUri(), transportAuthenticationProvider});


        try {
            Executor executor = Executor.newInstance();
            TransportAuthenticationContext context = new TransportAuthenticationContext();
            context.addAttribute("endpoint", replicationEndpoint);
            executor = transportAuthenticationProvider.authenticate(executor, context);

            deliverPackage(executor, replicationPackage, replicationEndpoint);

        } catch (Exception ex) {
            throw new ReplicationTransportException(ex);
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

    private void deliverPackage(Executor executor, ReplicationPackage replicationPackage,
                                ReplicationEndpoint replicationEndpoint) throws IOException {
        String type = replicationPackage.getType();


        Request req = Request.Post(replicationEndpoint.getUri()).useExpectContinue();

        if (useCustomHeaders) {
            String[] customizedHeaders = getCustomizedHeaders(customHeaders, replicationPackage.getAction(), replicationPackage.getPaths());
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
                inputStream = replicationPackage.createInputStream();
            }

            if (inputStream != null) {
                req = req.bodyStream(inputStream, ContentType.APPLICATION_OCTET_STREAM);
            }

            response = executor.execute(req);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        if (response != null) {
            Content content = response.returnContent();
            log.info("Replication content of type {} for {} delivered: {}", new Object[]{
                    type, Arrays.toString(replicationPackage.getPaths()), content});
        } else {
            throw new IOException("response is empty");
        }
    }

    private static void addHeader(Request req, String header) {
        int idx = header.indexOf(":");
        if (idx < 0) return;
        String headerName = header.substring(0, idx).trim();
        String headerValue = header.substring(idx + 1).trim();
        req.addHeader(headerName, headerValue);
    }
}