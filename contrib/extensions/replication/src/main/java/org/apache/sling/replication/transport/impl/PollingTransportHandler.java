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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageImporter;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * basic HTTP GET {@link TransportHandler}
 */
@Component(metatype = false)
@Service(value = TransportHandler.class)
@Property(name = "name", value = PollingTransportHandler.NAME)
public class PollingTransportHandler implements TransportHandler {

    public static final String NAME = "poll";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationPackageImporter replicationPackageImporter;

    @SuppressWarnings("unchecked")
    public void transport(ReplicationPackage replicationPackage,
                          ReplicationEndpoint replicationEndpoint,
                          TransportAuthenticationProvider<?, ?> transportAuthenticationProvider)
            throws ReplicationTransportException {
        if (log.isInfoEnabled()) {
            log.info("polling from {}", replicationEndpoint.getUri());
        }

        try {
            Executor executor = Executor.newInstance();
            TransportAuthenticationContext context = new TransportAuthenticationContext();
            context.addAttribute("endpoint", replicationEndpoint);
            executor = ((TransportAuthenticationProvider<Executor, Executor>) transportAuthenticationProvider)
                    .authenticate(executor, context);

            Request req = Request.Get(replicationEndpoint.getUri()).useExpectContinue();
            // TODO : add queue header

            // continuously requests package streams as long as type header is received with the response (meaning there's a package of a certain type)
            HttpResponse httpResponse;
            while ((httpResponse = executor.execute(req).returnResponse()).containsHeader(ReplicationHeader.TYPE.toString())) {
                HttpEntity entity = httpResponse.getEntity();
                Header typeHeader = httpResponse.getFirstHeader(ReplicationHeader.TYPE.toString());

                if (entity.getContentLength() > 0) {
                    replicationPackageImporter.scheduleImport(entity.getContent(), typeHeader.getValue());
                    if (log.isInfoEnabled()) {
                        log.info("scheduled import of package stream");
                    }

                } else {
                    if (log.isInfoEnabled()) {
                        log.info("nothing to fetch");
                    }
                }
            }
        } catch (Exception e) {
            throw new ReplicationTransportException(e);
        }

    }

    public boolean supportsAuthenticationProvider(TransportAuthenticationProvider<?, ?> transportAuthenticationProvider) {
        return transportAuthenticationProvider.canAuthenticate(Executor.class);
    }
}
