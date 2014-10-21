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

    public void deliverPackage(ResourceResolver resourceResolver, ReplicationPackage replicationPackage) throws ReplicationTransportException {
        String host = replicationEndpoint.getUri().getHost();
        if (replicationPackage instanceof HttpPackage && host.equals(
                ((HttpPackage) replicationPackage).getOrigin())) {
            log.info("skipping replication of package {} to same origin {}", replicationPackage.getId(), host);
        } else {
            log.info("delivering package {} to {} using auth {}",
                    new Object[]{
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

    }

    public List<ReplicationPackage> retrievePackages(final ResourceResolver resourceResolver, final ReplicationRequest replicationRequest) throws ReplicationTransportException {
        log.debug("polling from {}", replicationEndpoint.getUri());

        try {
            URI replicationURI = RequestUtils.appendReplicationRequest(replicationEndpoint.getUri(), replicationRequest);
            List<ReplicationPackage> result = new ArrayList<ReplicationPackage>();

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
                        HttpPackage httpPackage = new HttpPackage() {
                            public String getOrigin() {
                                return replicationEndpoint.getUri().getHost();
                            }

                            public String getId() {
                                return responsePackage.getId();
                            }

                            public String[] getPaths() {
                                return responsePackage.getPaths();
                            }

                            public String getAction() {
                                return responsePackage.getAction();
                            }

                            public String getType() {
                                return responsePackage.getType();
                            }

                            public InputStream createInputStream() throws IOException {
                                return responsePackage.createInputStream();
                            }

                            public long getLength() {
                                return responsePackage.getLength();
                            }

                            public void close() {
                                responsePackage.close();
                            }

                            public void delete() {
                                responsePackage.delete();
                            }
                        };

                        result.add(httpPackage);
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

    private interface HttpPackage extends ReplicationPackage {

        String getOrigin();

    }
}
