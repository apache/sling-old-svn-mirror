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
import javax.annotation.Nullable;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpResponseException;
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
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.transport.DistributionTransportSecret;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
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
    private static final String PACKAGE_INFO_PROPERTY_ORIGIN_URI = "internal.origin.uri";

    private final DefaultDistributionLog log;
    private final DistributionEndpoint distributionEndpoint;
    private final DistributionPackageBuilder packageBuilder;
    private final DistributionTransportSecretProvider secretProvider;
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
                try {
                    inputStream = DistributionPackageUtils.createStreamWithHeader(distributionPackage);

                    req = req.bodyStream(inputStream, ContentType.APPLICATION_OCTET_STREAM);

                    Response response = executor.execute(req);
                    response.returnContent(); // throws an error if HTTP status is >= 300

                } finally {
                    IOUtils.closeQuietly(inputStream);
                }

                log.debug("delivered packageId={}, endpoint={}", distributionPackage.getId(), distributionEndpoint.getUri());
            } catch (HttpHostConnectException e) {
                throw new RecoverableDistributionException("endpoint not available " + distributionEndpoint.getUri(), e);
            } catch (HttpResponseException e) {
                int statusCode = e.getStatusCode();
                if (statusCode == 404 || statusCode == 401) {
                    throw new RecoverableDistributionException("not enough rights for " + distributionEndpoint.getUri(), e);
                }
                throw new DistributionException(e);
            } catch (Exception e) {
                throw new DistributionException(e);

            }
        }
    }

    @Nullable
    public RemoteDistributionPackage retrievePackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest, @Nonnull DistributionTransportContext distributionContext) throws DistributionException {
        log.debug("pulling from {}", distributionEndpoint.getUri());

        try {
            URI distributionURI = RequestUtils.appendDistributionRequest(distributionEndpoint.getUri(), distributionRequest);

            Executor executor = getExecutor(distributionContext);

            // TODO : add queue parameter
            InputStream inputStream = HttpTransportUtils.fetchNextPackage(executor, distributionURI);

            if (inputStream == null) {
                return null;
            }

            final DistributionPackage responsePackage = packageBuilder.readPackage(resourceResolver, inputStream);
            responsePackage.getInfo().put(PACKAGE_INFO_PROPERTY_ORIGIN_URI, distributionURI);
            log.debug("pulled package with info {}", responsePackage.getInfo());

            return new DefaultRemoteDistributionPackage(responsePackage, executor, distributionURI);
        } catch (HttpHostConnectException e) {
            log.debug("could not connect to {} - skipping", distributionEndpoint.getUri());
        } catch (Exception ex) {
            log.error("cannot retrieve packages", ex);
        }

        return null;
    }

    private Executor authenticate(DistributionTransportSecret secret, Executor executor) {
        Map<String, String> credentialsMap = secret.asCredentialsMap();
        if (credentialsMap != null) {
            URI uri = distributionEndpoint.getUri();
            executor = executor.auth(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()),
                    credentialsMap.get(USERNAME), credentialsMap.get(PASSWORD)).authPreemptive(
                    new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()));
            log.debug("authenticate user={}, endpoint={}", secret.asCredentialsMap().get(USERNAME), distributionEndpoint.getUri());
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
