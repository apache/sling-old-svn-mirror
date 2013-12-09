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

import java.io.IOException;
import java.util.Arrays;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * basic HTTP POST {@link TransportHandler}
 */
@Component(metatype = false)
@Service(value = TransportHandler.class)
@Property(name = "name", value = HttpTransportHandler.NAME)
public class HttpTransportHandler implements TransportHandler {

    public static final String NAME = "http";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @SuppressWarnings("unchecked")
    public void transport(ReplicationPackage replicationPackage,
                          ReplicationEndpoint replicationEndpoint,
                          TransportAuthenticationProvider<?, ?> transportAuthenticationProvider)
            throws ReplicationTransportException {
        if (log.isInfoEnabled()) {
            log.info("delivering package {} to {} using auth {}",
                    new Object[]{replicationPackage.getId(),
                            replicationEndpoint.getUri(), transportAuthenticationProvider});
        }
        try {
            Executor executor = Executor.newInstance();
            TransportAuthenticationContext context = new TransportAuthenticationContext();
            context.addAttribute("endpoint", replicationEndpoint);
            executor = ((TransportAuthenticationProvider<Executor, Executor>) transportAuthenticationProvider)
                    .authenticate(executor, context);

            String[] paths = replicationPackage.getPaths();
            String type = replicationPackage.getType();
            String pathsString = Arrays.toString(paths);
            Request req = Request.Post(replicationEndpoint.getUri()).useExpectContinue()
                    .addHeader(ReplicationHeader.TYPE.toString(), type);
            if (replicationPackage.getInputStream() != null) {
                req = req.bodyStream(replicationPackage.getInputStream(),
                        ContentType.APPLICATION_OCTET_STREAM);
            }
            Response response = executor.execute(req);
            if (response != null) {
                Content content = response.returnContent();
                if (log.isInfoEnabled()) {
                    log.info("Replication content of type {} for {} delivered: {}", new Object[]{
                            type, pathsString, content});
                }
            }
            else {
                throw new IOException("response is empty");
            }
        } catch (Exception e) {
            throw new ReplicationTransportException(e);
        }
    }

    public boolean supportsAuthenticationProvider(TransportAuthenticationProvider<?, ?> transportAuthenticationProvider) {
        return transportAuthenticationProvider.canAuthenticate(Executor.class);
    }
}