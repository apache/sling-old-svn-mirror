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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.common.RecoverableDistributionException;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.transport.DistributionTransportSecret;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.core.DistributionTransportContext;
import org.apache.sling.distribution.transport.core.DistributionTransport;
import org.apache.sling.distribution.transport.core.DistributionPackageProxy;
import org.apache.sling.distribution.util.RequestUtils;

/**
 * default HTTP implementation of {@link DistributionTransport}
 */
public class SimpleHttpDistributionTransport implements DistributionTransport {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String EXECUTOR_CONTEXT_KEY_PREFIX = "ExecutorContextKey";


    /**
     * distribution package origin uri
     */
    public static String PACKAGE_INFO_PROPERTY_ORIGIN_URI = "internal.origin.uri";

    protected final DefaultDistributionLog log;
    private final DistributionEndpoint distributionEndpoint;
    private final DistributionPackageBuilder packageBuilder;
    protected final DistributionTransportSecretProvider secretProvider;
    private final String contextKeyExecutor;

    public SimpleHttpDistributionTransport(DefaultDistributionLog log, DistributionEndpoint distributionEndpoint,
                                           DistributionPackageBuilder packageBuilder,
                                           DistributionTransportSecretProvider secretProvider) {
        this.log = log;

        this.distributionEndpoint = distributionEndpoint;
        this.packageBuilder = packageBuilder;
        this.secretProvider = secretProvider;
        this.contextKeyExecutor = EXECUTOR_CONTEXT_KEY_PREFIX + "_" + getHostAndPort(distributionEndpoint.getUri()) + "_" + UUID.randomUUID();
    }

    public void deliverPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage, @Nonnull DistributionTransportContext distributionContext) throws DistributionException {
        String hostAndPort = getHostAndPort(distributionEndpoint.getUri());

        URI packageOrigin = distributionPackage.getInfo().get(PACKAGE_INFO_PROPERTY_ORIGIN_URI, URI.class);

        if (packageOrigin != null && hostAndPort.equals(getHostAndPort(packageOrigin))) {
            log.debug("skipping distribution of package {} to same origin {}", distributionPackage.getId(), hostAndPort);
        } else {


            try {
                Executor executor = getExecutor(distributionContext);

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
                log.debug("delivered package {} to {}", distributionPackage.getId(), distributionEndpoint.getUri());
            } catch (HttpHostConnectException e) {
                log.info("could not connect to {} - retrying", distributionEndpoint.getUri());
                throw new RecoverableDistributionException(e);
            } catch (Exception ex) {
                throw new DistributionException(ex);
            }
        }

    }

    @Nonnull
    public DistributionPackageProxy retrievePackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest, @Nonnull DistributionTransportContext distributionContext) throws DistributionException {
        log.debug("pulling from {}", distributionEndpoint.getUri());
        List<DistributionPackage> result = new ArrayList<DistributionPackage>();


        try {
            URI distributionURI = RequestUtils.appendDistributionRequest(distributionEndpoint.getUri(), distributionRequest);

            // TODO : executor should be cached and reused


            Executor executor = getExecutor(distributionContext);

//            Request req = Request.Post(distributionURI).useExpectContinue();

            // TODO : add queue parameter

            // continuously requests package streams as long as type header is received with the response (meaning there's a package of a certain type)
            final Map<String, String> headers = new HashMap<String, String>();


            InputStream inputStream = HttpTransportUtils.fetchNextPackage(executor, distributionURI, headers);

            if (inputStream == null) {
                return null;
            }

            final DistributionPackage responsePackage = packageBuilder.readPackage(resourceResolver, inputStream);
            if (responsePackage != null) {
                responsePackage.getInfo().put(PACKAGE_INFO_PROPERTY_ORIGIN_URI, distributionURI);
                log.debug("pulled package with info {}", responsePackage.getInfo());


                String originalId = headers.get(HttpTransportUtils.HEADER_DISTRIBUTION_ORIGINAL_ID);

                DistributionPackageProxy remotePackage = new DefaultDistributionPackageProxy(responsePackage, executor, distributionURI, originalId);

                return remotePackage;
            } else {
                log.warn("responsePackage is null");
            }
        } catch (HttpHostConnectException e) {
            log.info("could not connect to {} - skipping", distributionEndpoint.getUri());
        } catch (Exception ex) {
            log.error("cannot retrieve packages", ex);
        }

        return null;
    }

    protected Executor authenticate(DistributionTransportSecret secret, Executor executor) {
        Map<String, String> credentialsMap = secret.asCredentialsMap();
        if (credentialsMap != null) {
            executor = executor.auth(new HttpHost(distributionEndpoint.getUri().getHost(), distributionEndpoint.getUri().getPort()),
                    credentialsMap.get(USERNAME), credentialsMap.get(PASSWORD)).authPreemptive(
                    new HttpHost(distributionEndpoint.getUri().getHost(), distributionEndpoint.getUri().getPort()));
            log.debug("authenticated executor HTTP client with user and password {}", secret.asCredentialsMap().get(USERNAME));
        }
        return executor;
    }

    private String getHostAndPort(URI uri) {
        return uri.getHost() + ":" + uri.getPort();
    }


    private Executor getExecutor(DistributionTransportContext distributionContext) {
        if (distributionContext.containsKey(contextKeyExecutor)) {
            return distributionContext.get(contextKeyExecutor, Executor.class);
        }

        Executor executor = Executor.newInstance();

        DistributionTransportSecret secret = secretProvider.getSecret(distributionEndpoint.getUri());
        executor = authenticate(secret, executor);

        distributionContext.put(contextKeyExecutor, executor);

        return executor;

    }

}
