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
import org.apache.http.client.fluent.Response;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilderProvider;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.AuthenticationContext;
import org.apache.sling.replication.transport.authentication.AuthenticationHandler;
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
    private ReplicationPackageBuilderProvider packageBuilderProvider;

    @SuppressWarnings("unchecked")
    public void transport(ReplicationPackage replicationPackage,
                    ReplicationEndpoint replicationEndpoint,
                    AuthenticationHandler<?, ?> authenticationHandler)
                    throws ReplicationTransportException {
        if (log.isInfoEnabled()) {
            log.info("polling from {}", replicationEndpoint.getUri());
        }
        try {
            Executor executor = Executor.newInstance();
            AuthenticationContext context = new AuthenticationContext();
            context.addAttribute("endpoint", replicationEndpoint);
            executor = ((AuthenticationHandler<Executor, Executor>) authenticationHandler)
                            .authenticate(executor, context);

            Request req = Request.Get(replicationEndpoint.getUri()).useExpectContinue();
            // TODO : missing queue header
            Response response = executor.execute(req);
            HttpResponse httpResponse = response.returnResponse();
            HttpEntity entity = httpResponse.getEntity();
            Header typeHeader = httpResponse.getFirstHeader("type");
            Header pathsHeader = httpResponse.getFirstHeader("path");
            Header actionHeader = httpResponse.getFirstHeader("action");

            if (typeHeader != null && pathsHeader != null && actionHeader != null) {
                String type = typeHeader.getValue();
                String[] paths = Text.unescape(pathsHeader.getValue()).replace("[", "")
                                .replace("]", "").split(", ");
                ReplicationActionType action = ReplicationActionType.valueOf(actionHeader
                                .getValue());
                ReplicationRequest replicationRequest = new ReplicationRequest(
                                System.currentTimeMillis(), action, paths);

                ReplicationPackage readPackage = packageBuilderProvider
                                .getReplicationPacakageBuilder(type).readPackage(
                                                replicationRequest, entity.getContent(), true);

                if (log.isInfoEnabled()) {
                    log.info("package {} fetched and installed", readPackage.getId());
                }

            } else {
                if (log.isInfoEnabled()) {
                    log.info("nothing to fetch");
                }
            }
        } catch (Exception e) {
            throw new ReplicationTransportException(e);
        }

    }

    public boolean supportsAuthenticationHandler(AuthenticationHandler<?, ?> authenticationHandler) {
        return authenticationHandler.canAuthenticate(Executor.class);
    }
}
