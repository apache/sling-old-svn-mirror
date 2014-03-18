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

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * basic HTTP GET {@link TransportHandler}
 */
public class PollingTransportHandler extends AbstractTransportHandler
        implements TransportHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final TransportAuthenticationProvider<Executor, Executor> transportAuthenticationProvider;
    private final int pollItems;

    public PollingTransportHandler(int pollItems,
                                   TransportAuthenticationProvider<Executor, Executor> transportAuthenticationProvider,
                                   ReplicationEndpoint[] replicationEndpoints){
        super(replicationEndpoints, TransportEndpointStrategyType.All);
        this.pollItems = pollItems;
        this.transportAuthenticationProvider = transportAuthenticationProvider;
    }

    @Override
    public void deliverPackageToEndpoint(ReplicationPackage replicationPackage,
                                         ReplicationEndpoint replicationEndpoint,
                                         ReplicationQueueProcessor responseProcessor)
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
            ReplicationQueueItem queueItem = readPackageHeaders(httpResponse);

            if(responseProcessor != null)
                responseProcessor.process("poll", queueItem);
            polls--;
        }

    }


     ReplicationQueueItem readPackageHeaders(HttpResponse httpResponse) throws IOException {
        Header typeHeader = httpResponse.getFirstHeader(ReplicationHeader.TYPE.toString());
        Header actionHeader = httpResponse.getFirstHeader(ReplicationHeader.ACTION.toString());
        Header[] pathHeaders = httpResponse.getHeaders(ReplicationHeader.PATH.toString());
        List<String> pathList = new ArrayList<String>();

        for(Header pathHeader : pathHeaders){
            pathHeader.getValue();
            pathList.add(pathHeader.getValue());
        }

        HttpEntity entity = httpResponse.getEntity();
        byte[] bytes = IOUtils.toByteArray(entity.getContent());


        return new ReplicationQueueItem(pathList.toArray(new String[0]),
                actionHeader.getValue(),
                typeHeader.getValue(),
                bytes);

    }

    @Override
    protected boolean validateEndpoint(ReplicationEndpoint endpoint) {
        return true;
    }
}
