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
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.transport.core.DistributionTransport;
import org.apache.sling.distribution.transport.core.DistributionTransportException;
import org.apache.sling.distribution.transport.DistributionTransportSecret;
import org.apache.sling.distribution.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleHttpDistributionTransport implements DistributionTransport {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DistributionEndpoint distributionEndpoint;
    private final DistributionPackageBuilder packageBuilder;
    private final int maxNumberOfPackages;

    public SimpleHttpDistributionTransport(DistributionEndpoint distributionEndpoint,
                                           DistributionPackageBuilder packageBuilder,
                                           int maxNumberOfPackages) {

        this.distributionEndpoint = distributionEndpoint;
        this.packageBuilder = packageBuilder;
        this.maxNumberOfPackages = maxNumberOfPackages;
    }

    public void deliverPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage,
                               @Nonnull DistributionTransportSecret secret) throws DistributionTransportException {
        String hostAndPort = getHostAndPort(distributionEndpoint.getUri());

        URI packageOrigin = distributionPackage.getInfo().getOrigin();
        if (packageOrigin != null && hostAndPort.equals(getHostAndPort(packageOrigin))) {
            log.info("skipping distribution of package {} to same origin {}", distributionPackage.getId(), hostAndPort);
        } else {
            log.info("delivering package {} to {} using secret {}", new Object[]{
                    distributionPackage.getId(),
                    distributionEndpoint.getUri(),
                    secret
            });

            try {
                Executor executor = Executor.newInstance();

                executor = authenticate(secret, executor);

                Request req = Request.Post(distributionEndpoint.getUri()).useExpectContinue();

                InputStream inputStream = null;
                Response response = null;
                try {
                    inputStream = distributionPackage.createInputStream();

                    req = req.bodyStream(inputStream, ContentType.APPLICATION_OCTET_STREAM);
                    response = executor.execute(req);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }

                Content content = response.returnContent();
                log.info("distribution content of type {} for {} delivered: {}", new Object[]{
                        distributionPackage.getType(),
                        Arrays.toString(distributionPackage.getInfo().getPaths()),
                        content
                });
            } catch (Exception ex) {
                throw new DistributionTransportException(ex);
            }
        }

    }

    @Nonnull
    public List<DistributionPackage> retrievePackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest
            distributionRequest, @Nonnull DistributionTransportSecret secret) throws DistributionTransportException {
        log.debug("pulling from {}", distributionEndpoint.getUri());

        try {
            URI distributionURI = RequestUtils.appendDistributionRequest(distributionEndpoint.getUri(), distributionRequest);
            List<DistributionPackage> result = new ArrayList<DistributionPackage>();

            // TODO : executor should be cached and reused

            Executor executor = Executor.newInstance();

            executor = authenticate(secret, executor);

            Request req = Request.Post(distributionURI).useExpectContinue();

            // TODO : add queue parameter

            // continuously requests package streams as long as type header is received with the response (meaning there's a package of a certain type)
            HttpResponse httpResponse;
            try {

                int pulls = 0;
                while ((httpResponse = executor.execute(req).returnResponse())
                        .getStatusLine().getStatusCode() == 200
                        && pulls < maxNumberOfPackages) {
                    HttpEntity entity = httpResponse.getEntity();
                    if (entity != null) {
                        final DistributionPackage responsePackage = packageBuilder.readPackage(resourceResolver, entity.getContent());
                        if (responsePackage != null) {
                            responsePackage.getInfo().setOrigin(distributionURI);
                            result.add(responsePackage);
                        } else {
                            log.warn("responsePackage is null");
                        }

                        pulls++;
                    } else {
                        log.info("");
                        break;
                    }
                }

                log.info("pulled {} packages from {}", pulls, distributionEndpoint.getUri());

            } catch (HttpHostConnectException e) {
                log.info("could not connect to {} - skipping", distributionEndpoint.getUri());
            }

            return result;

        } catch (Exception ex) {
            log.error("cannot retrieve packages", ex);

            throw new DistributionTransportException(ex);
        }

    }

    protected Executor authenticate(DistributionTransportSecret secret, Executor executor) {
        Map<String, String> credentialsMap = secret.asCredentialsMap();
        if (credentialsMap != null) {
            executor = executor.auth(new HttpHost(distributionEndpoint.getUri().getHost(), distributionEndpoint.getUri().getPort()),
                    credentialsMap.get("username"), credentialsMap.get("password")).authPreemptive(
                    new HttpHost(distributionEndpoint.getUri().getHost(), distributionEndpoint.getUri().getPort()));
            log.debug("authenticated executor HTTP client with user and password");
        }
        return executor;
    }

    private String getHostAndPort(URI uri) {
        return uri.getHost() + ":" + uri.getPort();
    }

}
