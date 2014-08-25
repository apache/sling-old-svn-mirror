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
package org.apache.sling.replication.rule.impl;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.*;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.resources.ReplicationConstants;
import org.apache.sling.replication.rule.ReplicationRequestHandler;
import org.apache.sling.replication.rule.ReplicationRule;
import org.apache.sling.replication.transport.ReplicationTransportHandler;
import org.apache.sling.replication.transport.impl.ReplicationTransportConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.replication.rule.ReplicationRule} to trigger replication upon reception of server sent events
 * on a certain queue
 */
@Component(immediate = true, label = "Rule for listening on Server Sent Events for Queues")
@Service(value = ReplicationRule.class)
public class ReplicateOnQueueEventRule implements ReplicationRule {

    @Property(label = "Name", value = "remote", propertyPrivate = true)
    private static final String NAME = "name";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String SIGNATURE = "remote trigger on {path} with user {user} and password {password}";

    private static final String SIGNATURE_REGEX = "remote\\strigger\\son\\s([^\\s]+)\\swith\\suser\\s([^\\s]+)\\sand\\spassword\\s([^\\s]+)";

    private static final Pattern signaturePattern = Pattern.compile(SIGNATURE_REGEX);

    @Reference
    private Scheduler scheduler;

    private BundleContext context;

    private Map<String, Future<HttpResponse>> requests;

    @Activate
    protected void activate(BundleContext context, Map<String, ?> config) {
        this.context = context;
        this.requests = new ConcurrentHashMap<String, Future<HttpResponse>>();
    }

    public String getSignature() {
        return SIGNATURE;
    }

    public boolean signatureMatches(String ruleString) {
        return ruleString.matches(SIGNATURE_REGEX);
    }

    public void apply(String handleId, ReplicationRequestHandler agent, String ruleString) {
        Matcher matcher = signaturePattern.matcher(ruleString);
        if (matcher.find()) {
            String remotePath = matcher.group(1);
            String user = matcher.group(2);
            String password = matcher.group(3);



            try {
                log.info("applying queue event replication rule");

                ScheduleOptions options = scheduler.NOW();
                options.name(handleId);
                scheduler.schedule(new EventBasedReplication(handleId, agent, remotePath, user, password), options);

            } catch (Exception e) {
                log.error("{}", e);
                log.error("cannot apply rule {} to agent {}", ruleString, agent);
            }

        }
    }

    public void undo(String handleId) {
        Future<HttpResponse> httpResponseFuture = requests.remove(handleId);
        if (httpResponseFuture != null) {
            httpResponseFuture.cancel(true);
        }
    }

    private class SSEResponseConsumer extends BasicAsyncResponseConsumer {

        private final String handleId;
        private final ReplicationRequestHandler agent;


        private SSEResponseConsumer(String handleId, ReplicationRequestHandler agent) {
            this.handleId = handleId;
            this.agent = agent;

        }

        @Override
        protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
            log.info("complete ? ", decoder.isCompleted());
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            decoder.read(buffer);
            log.info("content {} received {},{}", new Object[]{buffer, decoder, ioctrl});
            log.info("event received");

            ReplicationRequest replicationRequest = new ReplicationRequest(System.currentTimeMillis(), ReplicationActionType.POLL, null);
            agent.execute(replicationRequest);
            log.info("replication request to agent {} sent ({} on {})", new Object[]{
                    handleId,
                    replicationRequest.getAction(),
                    replicationRequest.getPaths()});

            super.onContentReceived(decoder, ioctrl);
        }

        @Override
        protected void onResponseReceived(HttpResponse response) throws IOException {
            log.info("response received {}", response);
            super.onResponseReceived(response);
        }
    }



    private class EventBasedReplication implements Runnable {
        private final String handleId;
        private final ReplicationRequestHandler agent;
        private final String targetTransport;
        private final String userName;
        private final String password;

        public EventBasedReplication(String handleId, ReplicationRequestHandler agent, String targetTransport, String userName, String password) {
            this.handleId = handleId;
            this.agent = agent;
            this.targetTransport = targetTransport;
            this.userName = userName;
            this.password = password;
        }

        public void run() {
            try {


                log.info("getting event from {}", targetTransport + "?sec=600");

                URI eventEndpoint = URI.create(targetTransport);


                log.info("preparing request");
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(eventEndpoint.getHost(), eventEndpoint.getPort()),
                        new UsernamePasswordCredentials(userName, password));
                CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom()
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .build();
                try {
                    HttpGet get = new HttpGet(eventEndpoint);
                    HttpHost target = URIUtils.extractHost(get.getURI());
                    BasicAsyncRequestProducer basicAsyncRequestProducer = new BasicAsyncRequestProducer(target, get);
                    httpClient.start();
                    log.info("sending request");
                    Future<HttpResponse> futureResponse = httpClient.execute(
                            basicAsyncRequestProducer,
                            new SSEResponseConsumer(handleId, agent), null);
                    requests.put(handleId, futureResponse);
                    futureResponse.get();
                } finally {
                    httpClient.close();
                }
                log.info("request finished");
            } catch (Exception e) {
                log.error("cannot execute event based replication");
            }
        }
    }
}
