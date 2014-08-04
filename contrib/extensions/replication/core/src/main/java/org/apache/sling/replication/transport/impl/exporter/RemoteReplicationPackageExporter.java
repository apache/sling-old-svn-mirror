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
package org.apache.sling.replication.transport.impl.exporter;

import org.apache.felix.scr.annotations.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.*;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProviderFactory;
import org.apache.sling.replication.transport.authentication.impl.UserCredentialsTransportAuthenticationProviderFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Default implementation of {@link org.apache.sling.replication.serialization.ReplicationPackageExporter}
 */
@Component(label = "Remote Replication Package Exporter", configurationFactory = true)
@Service(value = ReplicationPackageExporter.class)
public class RemoteReplicationPackageExporter implements ReplicationPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property
    private static final String NAME = "name";

    @Property(name = ReplicationAgentConfiguration.TRANSPORT_AUTHENTICATION_FACTORY)
    @Reference(name = "TransportAuthenticationProviderFactory", policy = ReferencePolicy.DYNAMIC)
    private TransportAuthenticationProviderFactory transportAuthenticationProviderFactory;


    @Property(label = "Target ReplicationPackageBuilder", name = "ReplicationPackageBuilder.target")
    @Reference(name = "ReplicationPackageBuilder", policy = ReferencePolicy.STATIC)
    private ReplicationPackageBuilder packageBuilder;

    TransportAuthenticationProvider<Executor, Executor>  transportAuthenticationProvider;
    ReplicationEndpoint replicationEndpoint;

    @Activate
    protected void activate(BundleContext context, Map<String, ?> config) throws Exception {

        Map<String, String> authenticationProperties = PropertiesUtil.toMap(config.get(ReplicationAgentConfiguration.AUTHENTICATION_PROPERTIES), new String[0]);

        transportAuthenticationProvider = (TransportAuthenticationProvider<Executor, Executor>) transportAuthenticationProviderFactory.createAuthenticationProvider(authenticationProperties);

        String[] endpoints = PropertiesUtil.toStringArray(config.get(ReplicationAgentConfiguration.ENDPOINT), new String[0]);

        replicationEndpoint = new ReplicationEndpoint(endpoints[0]);

    }

    @Deactivate
    protected void deactivate() {
    }


    public ReplicationPackage exportPackage(ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {

        try {
            return pollPackageFromEndpoint(replicationRequest, replicationEndpoint);
        } catch (Exception e) {
            throw new ReplicationPackageBuildingException(e);
        }
    }

    public ReplicationPackage exportPackageById(String replicationPackageId) {
        return packageBuilder.getPackage(replicationPackageId);
    }


    private ReplicationPackage pollPackageFromEndpoint(ReplicationRequest replicationRequest, ReplicationEndpoint replicationEndpoint)
            throws Exception {
        log.debug("polling from {}", replicationEndpoint.getUri());


        Executor executor = Executor.newInstance();
        TransportAuthenticationContext context = new TransportAuthenticationContext();
        context.addAttribute("endpoint", replicationEndpoint);
        executor = transportAuthenticationProvider.authenticate(executor, context);

        Request req = Request.Post(replicationEndpoint.getUri())
                .addHeader(ReplicationHeader.ACTION.toString(), ReplicationActionType.POLL.getName())
                .useExpectContinue();
        // TODO : add queue parameter

        // continuously requests package streams as long as type header is received with the response (meaning there's a package of a certain type)
        HttpResponse httpResponse;
        try {
            httpResponse = executor.execute(req).returnResponse();
            if (httpResponse.containsHeader(ReplicationHeader.TYPE.toString())) {
                ReplicationPackage responsePackage = packageBuilder.readPackage(httpResponse.getEntity().getContent());

                return responsePackage;
            }
        } catch (HttpHostConnectException e) {
            log.warn("could not connect to {} - skipping", replicationEndpoint.getUri());
        }

        return null;

    }

}
