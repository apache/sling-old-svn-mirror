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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageImporter;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * basic HTTP GET {@link TransportHandler}
 */
public class PollingTransportHandler extends AbstractTransportHandler
        implements TransportHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final TransportAuthenticationProvider<Executor, Executor> transportAuthenticationProvider;
    private final ReplicationPackageImporter replicationPackageImporter;
    private final int pollItems;

    public PollingTransportHandler(ReplicationPackageImporter replicationPackageImporter,
                                   int pollItems,
                                   TransportAuthenticationProvider<Executor, Executor> transportAuthenticationProvider,
                                   ReplicationEndpoint[] replicationEndpoints){
        super(replicationEndpoints, TransportEndpointStrategyType.All);

        this.replicationPackageImporter = replicationPackageImporter;
        this.pollItems = pollItems;
        this.transportAuthenticationProvider = transportAuthenticationProvider;
    }

    @Override
    public void deliverPackageToEndpoint(ReplicationPackage replicationPackage, ReplicationEndpoint replicationEndpoint)
            throws Exception {
        log.info("polling from {}", replicationEndpoint.getUri());


        Executor executor = Executor.newInstance();
        TransportAuthenticationContext context = new TransportAuthenticationContext();
        context.addAttribute("endpoint", replicationEndpoint);
        executor = transportAuthenticationProvider.authenticate(executor, context);

        Request req = Request.Post(replicationEndpoint.getUri())
                .addHeader(ReplicationHeader.ACTION.toString(), ReplicationActionType.POLL.getName())
                .useExpectContinue();
        // TODO : add queue header

        int polls = pollItems;

        // continuously requests package streams as long as type header is received with the response (meaning there's a package of a certain type)
        HttpResponse httpResponse;
        while ((httpResponse = executor.execute(req).returnResponse()).containsHeader(ReplicationHeader.TYPE.toString())
                && polls != 0) {
            HttpEntity entity = httpResponse.getEntity();
            Header typeHeader = httpResponse.getFirstHeader(ReplicationHeader.TYPE.toString());

            if (entity.getContentLength() > 0) {
                replicationPackageImporter.scheduleImport(entity.getContent(), typeHeader.getValue());
                polls--;
                log.info("scheduled import of package stream");

            } else {
                log.info("nothing to fetch");
                break;
            }
        }

    }

    @Override
    protected boolean validateEndpoint(ReplicationEndpoint endpoint) {
        return true;
    }
}
