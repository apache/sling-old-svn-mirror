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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.ReplicationTransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleHttpReplicationTransportHandler implements ReplicationTransportHandler {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpReplicationTransportHandler.class);
    private final TransportAuthenticationProvider<Executor, Executor> transportAuthenticationProvider;
    private final ReplicationEndpoint replicationEndpoint;
    private final ReplicationPackageBuilder packageBuilder;
    private final int maxNumberOfPackages;

    public SimpleHttpReplicationTransportHandler(TransportAuthenticationProvider<Executor, Executor> transportAuthenticationProvider,
                                                 ReplicationEndpoint replicationEndpoint,
                                                 ReplicationPackageBuilder packageBuilder,
                                                 int maxNumberOfPackages) {

        if (transportAuthenticationProvider == null) {
            throw new IllegalArgumentException("The authentication provider is required");
        }

        if (!transportAuthenticationProvider.canAuthenticate(Executor.class)) {
            throw new IllegalArgumentException("Authentication provider cannot authenticate Executor");
        }

        this.transportAuthenticationProvider = transportAuthenticationProvider;
        this.replicationEndpoint = replicationEndpoint;
        this.packageBuilder = packageBuilder;
        this.maxNumberOfPackages = maxNumberOfPackages;
    }

    public void deliverPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull ReplicationPackage replicationPackage) throws ReplicationTransportException {
        String hostAndPort = getHostAndPort(replicationEndpoint.getUri());

        if (hostAndPort.equals(replicationPackage.getInfo().getOrigin())) {
            log.info("skipping replication of package {} to same origin {}", replicationPackage.getId(), hostAndPort);
            return;
        }


        log.info("delivering package {} to {} using auth {}", new Object[]{
                        replicationPackage.getId(),
                        replicationEndpoint.getUri(),
                        transportAuthenticationProvider
        });

        try {
            Executor executor = Executor.newInstance();

            TransportAuthenticationContext context = new TransportAuthenticationContext();
            context.addAttribute("endpoint", replicationEndpoint);
            executor = transportAuthenticationProvider.authenticate(executor, context);

            Request req = Request.Post(replicationEndpoint.getUri()).useExpectContinue();

            InputStream inputStream = null;
            Response response = null;
            try {

                inputStream = replicationPackage.createInputStream();

                req = req.bodyStream(inputStream, ContentType.APPLICATION_OCTET_STREAM);
                response = executor.execute(req);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }

            if (response != null) {
                Content content = response.returnContent();
                log.info("Replication content of type {} for {} delivered: {}", new Object[]{
                        replicationPackage.getType(),
                        Arrays.toString(replicationPackage.getPaths()),
                        content
                });
            } else {
                throw new IOException("response is empty");
            }
        } catch (Exception ex) {
            throw new ReplicationTransportException(ex);
        }

    }

    @Nonnull
    public List<ReplicationPackage> retrievePackages(@Nonnull final ResourceResolver resourceResolver, @Nonnull final ReplicationRequest replicationRequest) throws ReplicationTransportException {
        log.debug("polling from {}", replicationEndpoint.getUri());

        try {
            URI replicationURI = RequestUtils.appendReplicationRequest(replicationEndpoint.getUri(), replicationRequest);
            List<ReplicationPackage> result = new ArrayList<ReplicationPackage>();

            // TODO : executor should be cached and reused

            Executor executor = Executor.newInstance();
            TransportAuthenticationContext context = new TransportAuthenticationContext();
            context.addAttribute("endpoint", replicationEndpoint);
            executor = transportAuthenticationProvider.authenticate(executor, context);

            Request req = Request.Post(replicationURI).useExpectContinue();

            // TODO : add queue parameter

            // continuously requests package streams as long as type header is received with the response (meaning there's a package of a certain type)
            HttpResponse httpResponse;
            try {

                int polls = 0;
                while ((httpResponse = executor.execute(req).returnResponse())
                        .getStatusLine().getStatusCode() == 200
                        && polls < maxNumberOfPackages) {
                    HttpEntity entity = httpResponse.getEntity();
                    if (entity != null) {
                        final ReplicationPackage responsePackage = packageBuilder.readPackage(resourceResolver, entity.getContent());
                        if (responsePackage != null) {
                            String origin = getHostAndPort(replicationURI);
                            responsePackage.getInfo().setOrigin(origin);
                            result.add(responsePackage);
                        }
                        else {
                            log.warn("responsePackage is null");
                        }

                        polls++;
                    } else {
                        log.info("");
                        break;
                    }
                }

                log.info("polled {} packages from {}", polls, replicationEndpoint.getUri());

            } catch (HttpHostConnectException e) {
                log.info("could not connect to {} - skipping", replicationEndpoint.getUri());
            }

            return result;

        } catch (Exception ex) {
            throw new ReplicationTransportException(ex);
        }

    }

    private String getHostAndPort(URI uri) {
        return uri.getHost() + ":" + uri.getPort();
    }

}
