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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.transport.DistributionTransportSecret;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SimpleHttpDistributionTransport}
 */
@Ignore
public class SimpleHttpDistributionTransportTest {

    @Test
    public void testDeliverPackage() throws Exception {
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        Map<String, String> credentialsMap = new HashMap<String, String>();
        credentialsMap.put("username", "foo");
        credentialsMap.put("password", "foo");
        when(secret.asCredentialsMap()).thenReturn(credentialsMap);
        DistributionTransportSecretProvider secretProvider = mock(DistributionTransportSecretProvider.class);
        when(secretProvider.getSecret(any(URI.class))).thenReturn(secret);

        Executor executor = mock(Executor.class);
        Response response = mock(Response.class);
        when(executor.execute(any(Request.class))).thenReturn(response);
        DistributionEndpoint endpoint = new DistributionEndpoint("http://127.0.0.1:8080/some/resource");
        DistributionPackageBuilder packageBuilder = mock(DistributionPackageBuilder.class);
        int maxNoOfPackages = Integer.MAX_VALUE;
        SimpleHttpDistributionTransport simpleHttpDistributionTransport = new SimpleHttpDistributionTransport(
                endpoint, packageBuilder, secretProvider, maxNoOfPackages);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        when(distributionPackage.getInfo()).thenReturn(new DistributionPackageInfo());
        InputStream stream = mock(InputStream.class);
        when(distributionPackage.createInputStream()).thenReturn(stream);
        simpleHttpDistributionTransport.deliverPackage(resourceResolver, distributionPackage);
    }

    @Test
    public void testRetrievePackagesRemotelyFailing() throws Exception {
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        Map<String, String> credentialsMap = new HashMap<String, String>();
        credentialsMap.put("username", "foo");
        credentialsMap.put("password", "foo");
        when(secret.asCredentialsMap()).thenReturn(credentialsMap);
        DistributionTransportSecretProvider secretProvider = mock(DistributionTransportSecretProvider.class);
        when(secretProvider.getSecret(any(URI.class))).thenReturn(secret);

        Executor executor = mock(Executor.class);
        Response response = mock(Response.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(404);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(response.returnResponse()).thenReturn(httpResponse);
        when(executor.execute(any(Request.class))).thenReturn(response);
        DistributionEndpoint endpoint = new DistributionEndpoint("http://127.0.0.1:8080/some/resource");
        DistributionPackageBuilder packageBuilder = mock(DistributionPackageBuilder.class);
        int maxNoOfPackages = 1;
        SimpleHttpDistributionTransport simpleHttpDistributionTransport = new SimpleHttpDistributionTransport(
                endpoint, packageBuilder, secretProvider, maxNoOfPackages);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        List<DistributionPackage> packages = simpleHttpDistributionTransport.retrievePackages(resourceResolver, distributionRequest);
        assertNotNull(packages);
        assertTrue(packages.isEmpty());
    }

    @Test
    public void testRetrievePackagesRemotelyWorking() throws Exception {
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        Map<String, String> credentialsMap = new HashMap<String, String>();
        credentialsMap.put("username", "foo");
        credentialsMap.put("password", "foo");
        when(secret.asCredentialsMap()).thenReturn(credentialsMap);
        DistributionTransportSecretProvider secretProvider = mock(DistributionTransportSecretProvider.class);
        when(secretProvider.getSecret(any(URI.class))).thenReturn(secret);

        Executor executor = mock(Executor.class);
        Response response = mock(Response.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        HttpEntity entity = mock(HttpEntity.class);
        InputStream stream = new ByteArrayInputStream("package binary stuff".getBytes("UTF-8"));
        when(entity.getContent()).thenReturn(stream);
        when(httpResponse.getEntity()).thenReturn(entity);
        when(response.returnResponse()).thenReturn(httpResponse);
        when(executor.execute(any(Request.class))).thenReturn(response);
        DistributionEndpoint endpoint = new DistributionEndpoint("http://127.0.0.1:8080/some/resource");
        DistributionPackageBuilder packageBuilder = mock(DistributionPackageBuilder.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        when(distributionPackage.getInfo()).thenReturn(new DistributionPackageInfo());
        when(packageBuilder.readPackage(any(ResourceResolver.class), any(InputStream.class))).thenReturn(distributionPackage);
        int maxNoOfPackages = 1;
        SimpleHttpDistributionTransport simpleHttpDistributionTransport = new SimpleHttpDistributionTransport(
                endpoint, packageBuilder, secretProvider, maxNoOfPackages);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        List<DistributionPackage> packages = simpleHttpDistributionTransport.retrievePackages(resourceResolver, distributionRequest);
        assertNotNull(packages);
        assertFalse(packages.isEmpty());
        assertEquals(1, packages.size());
        assertNotNull(packages.get(0));
    }
}