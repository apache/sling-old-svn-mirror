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
package org.apache.sling.distribution.trigger.impl;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.DistributionEndpoint;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.trigger.DistributionTrigger} to trigger distribution upon reception of server sent events
 * on a certain URL
 */
public class RemoteEventDistributionTrigger implements DistributionTrigger {
    private final static String SCHEDULE_NAME = "remoteEventTrigger";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DistributionEndpoint endpoint;
    private final DistributionTransportSecretProvider distributionTransportSecretProvider;

    private Scheduler scheduler;

    private final Map<DistributionRequestHandler, Future<HttpResponse>> requests = new ConcurrentHashMap<DistributionRequestHandler, Future<HttpResponse>>();

    public RemoteEventDistributionTrigger(String endpoint, DistributionTransportSecretProvider distributionTransportSecretProvider, Scheduler scheduler) {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint is required");
        }

        if (distributionTransportSecretProvider == null) {
            throw new IllegalArgumentException("Authentication provider is required");
        }

        this.distributionTransportSecretProvider = distributionTransportSecretProvider;
        this.endpoint = new DistributionEndpoint(endpoint);
        this.scheduler = scheduler;
    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        try {
            log.info("applying remote event distribution trigger");

            ScheduleOptions options = scheduler.NOW();
            options.name(getJobName(requestHandler));
            options.canRunConcurrently(false);
            options.onLeaderOnly(true);
            scheduler.schedule(new EventBasedDistribution(requestHandler), options);
        } catch (Exception e) {
            throw new DistributionException("unable to register handler " + requestHandler, e);
        }
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        Future<HttpResponse> httpResponseFuture = requests.remove(requestHandler.toString());
        if (httpResponseFuture != null) {
            httpResponseFuture.cancel(true);
        }
        scheduler.unschedule(getJobName(requestHandler));
    }

    private String getJobName(DistributionRequestHandler requestHandler) {
        return SCHEDULE_NAME + requestHandler.toString();
    }

    private class SSEResponseConsumer extends BasicAsyncResponseConsumer {

        private final DistributionRequestHandler handler;

        private SSEResponseConsumer(DistributionRequestHandler handler) {
            this.handler = handler;
        }

        @Override
        protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
            log.debug("complete {}", decoder.isCompleted());
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            decoder.read(buffer);
            log.debug("content {} received {},{}", new Object[]{buffer, decoder, ioctrl});

            // TODO : currently it always triggers pull request on /, should this be configurable?
            DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.PULL, "/");
            handler.handle(null, distributionRequest);
            log.info("distribution request to agent {} sent ({} {})", new Object[]{
                    handler,
                    distributionRequest.getRequestType(),
                    distributionRequest.getPaths()});

            super.onContentReceived(decoder, ioctrl);
        }

        @Override
        protected void onResponseReceived(HttpResponse response) throws IOException {
            log.info("response received {}", response);
            super.onResponseReceived(response);
        }
    }

    private class EventBasedDistribution implements Runnable {
        private final DistributionRequestHandler requestHandler;

        public EventBasedDistribution(DistributionRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void run() {
            try {
                log.debug("getting events from {}", endpoint.getUri().toString());

                log.debug("preparing request");

                // TODO : http client should be cached and reused

                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

                Map<String, String> credentialsMap = distributionTransportSecretProvider.getSecret(endpoint.getUri()).asCredentialsMap();
                if (credentialsMap != null) {
                    String username = credentialsMap.get("username");
                    String password = credentialsMap.get("password");
                    credentialsProvider.setCredentials(new AuthScope(new HttpHost(endpoint.getUri().getHost(), endpoint.getUri().getPort())),
                            new UsernamePasswordCredentials(username, password));

                    final CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom()
                            .setDefaultCredentialsProvider(credentialsProvider)
                            .build();

                    HttpGet get = new HttpGet(endpoint.getUri());
                    HttpHost target = URIUtils.extractHost(get.getURI());
                    BasicAsyncRequestProducer basicAsyncRequestProducer = new BasicAsyncRequestProducer(target, get);
                    httpClient.start();
                    try {
                        log.debug("sending request");
                        Future<HttpResponse> futureResponse = httpClient.execute(
                                basicAsyncRequestProducer,
                                new SSEResponseConsumer(requestHandler), new FutureCallback<HttpResponse>() {
                                    public void completed(HttpResponse httpResponse) {
                                        log.debug("response received {}", httpResponse);
                                    }

                                    public void failed(Exception e) {
                                        log.warn("failed request {}", e.toString());
                                    }

                                    public void cancelled() {
                                        log.warn("request cancelled");
                                    }
                                });
                        requests.put(requestHandler, futureResponse);
                        futureResponse.get();

                    } catch (Exception e) {
                        log.warn("cannot communicate with {}", endpoint, e);
                    }
                    httpClient.close();
                    log.debug("request finished");
                }
            } catch (Exception e) {
                log.error("cannot run event based distribution {}", e);
            }
        }
    }

    public void disable() {

        for (Map.Entry<DistributionRequestHandler, Future<HttpResponse>> entry : requests.entrySet()) {
            entry.getValue().cancel(true);
            scheduler.unschedule(getJobName(entry.getKey()));
        }
    }
}
