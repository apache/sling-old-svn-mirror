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
package org.apache.sling.replication.transport.impl.importer;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageImporter;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProviderFactory;
import org.apache.sling.replication.transport.authentication.impl.UserCredentialsTransportAuthenticationProviderFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

/**
 * Default implementation of {@link org.apache.sling.replication.serialization.ReplicationPackageImporter}
 */
@Component(label = "Remote Replication Package Importer", configurationFactory = true)
@Service(value = ReplicationPackageImporter.class)
public class RemoteReplicationPackageImporter implements ReplicationPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property
    private static final String NAME = "name";

    @Property(name = ReplicationAgentConfiguration.TRANSPORT_AUTHENTICATION_FACTORY)
    @Reference(name = "TransportAuthenticationProviderFactory", policy = ReferencePolicy.DYNAMIC)
    private TransportAuthenticationProviderFactory transportAuthenticationProviderFactory;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    TransportAuthenticationProvider<Executor, Executor>  transportAuthenticationProvider;
    ReplicationEndpoint replicationEndpoint;

    @Activate
    protected void activate(BundleContext context, Map<String, ?> config) throws Exception {

        Map<String, String> authenticationProperties = PropertiesUtil.toMap(config.get(ReplicationAgentConfiguration.AUTHENTICATION_PROPERTIES), new String[0]);

        transportAuthenticationProvider = (TransportAuthenticationProvider<Executor, Executor>) transportAuthenticationProviderFactory.createAuthenticationProvider(authenticationProperties);

        String[] endpoints = PropertiesUtil.toStringArray(config.get(ReplicationAgentConfiguration.ENDPOINT), new String[0]);

        replicationEndpoint = new ReplicationEndpoint(endpoints[0]);
    }


    public boolean importPackage(ReplicationPackage replicationPackage) {

        try {
           deliverPackageToEndpoint(replicationPackage, replicationEndpoint);

            return true;
        } catch (Exception e) {

        }

        return false;
    }

    public ReplicationPackage readPackage(InputStream stream) throws ReplicationPackageReadingException {

        return null;
    }


    public void deliverPackageToEndpoint(ReplicationPackage replicationPackage,
                                         ReplicationEndpoint replicationEndpoint) throws Exception {
        log.info("delivering package {} to {} using auth {}",
                new Object[]{replicationPackage.getId(),
                        replicationEndpoint.getUri(), transportAuthenticationProvider});


        Executor executor = Executor.newInstance();
        TransportAuthenticationContext context = new TransportAuthenticationContext();
        context.addAttribute("endpoint", replicationEndpoint);
        executor =  transportAuthenticationProvider.authenticate(executor, context);

        Request req = Request.Post(replicationEndpoint.getUri()).useExpectContinue();


        InputStream inputStream = null;
        Response response = null;
        try{

            inputStream = replicationPackage.createInputStream();


            if(inputStream != null) {
                req = req.bodyStream(inputStream, ContentType.APPLICATION_OCTET_STREAM);
            }

            response = executor.execute(req);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }

        if (response != null) {
            Content content = response.returnContent();
            log.info("Replication content of type {} for {} delivered: {}", new Object[]{
                    replicationPackage.getType(), Arrays.toString(replicationPackage.getPaths()), content});
        }
        else {
            throw new IOException("response is empty");
        }
    }

}
