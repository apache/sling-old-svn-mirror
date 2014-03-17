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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
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
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.agent.ReplicationAgentConfigurationManager;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.resources.ReplicationConstants;
import org.apache.sling.replication.rule.ReplicationRule;
import org.apache.sling.replication.transport.TransportHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.replication.rule.ReplicationRule} to trigger
 */
@Component(immediate = true, label = "Rule for generating Server Sent Events for Queues")
@Service(value = ReplicationRule.class)
public class ReplicateOnQueueEventRule implements ReplicationRule {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String SIGNATURE = "queue event based {action} [on ${path}]";

    private static final String SIGNATURE_REGEX = "(queue\\sevent\\sbased)\\s(add|delete|poll)(\\s(on)\\s(\\/\\w+)+)?";

    private static final Pattern signaturePattern = Pattern.compile(SIGNATURE_REGEX);

    @Reference
    private ReplicationAgentConfigurationManager replicationAgentConfigurationManager;

    @Reference
    private Scheduler scheduler;

    private BundleContext context;

    private Map<String, Future<HttpResponse>> requests;

    @Activate
    protected void activate(BundleContext context) {
        this.context = context;
        this.requests = new ConcurrentHashMap<String, Future<HttpResponse>>();
    }

    public String getSignature() {
        return SIGNATURE;
    }

    public boolean signatureMatches(String ruleString) {
        return ruleString.matches(SIGNATURE_REGEX);
    }

    public void apply(String ruleString, ReplicationAgent agent) {
        Matcher matcher = signaturePattern.matcher(ruleString);
        if (matcher.find()) {
            String action = matcher.group(2);
            ReplicationActionType actionType = ReplicationActionType.fromName(action.toUpperCase());
            String path = matcher.group(5); // can be null
            try {
                log.info("applying queue event replication rule");
                // get configuration
                ReplicationAgentConfiguration configuration = replicationAgentConfigurationManager.getConfiguration(agent.getName());

                // get URI of the event queue
                String targetTransport = configuration.getTargetTransportHandler();

                log.info("found target transport {}", targetTransport);

                ScheduleOptions options = scheduler.NOW();
                options.name(agent.getName() + " " + ruleString);
                scheduler.schedule(new EventBasedReplication(agent, actionType, path, targetTransport), options);

            } catch (Exception e) {
                log.error("{}", e);
                log.error("cannot apply rule {} to agent {}", ruleString, agent);
            }

        }
    }

    public void undo(String ruleString, ReplicationAgent agent) {
        Future<HttpResponse> httpResponseFuture = requests.remove(agent.getName());
        if (httpResponseFuture != null) {
            httpResponseFuture.cancel(true);
        }
    }

    private class SSEResponseConsumer extends BasicAsyncResponseConsumer {

        private final ReplicationAgent agent;
        private final ReplicationActionType action;
        private final String path;

        private SSEResponseConsumer(ReplicationAgent agent, ReplicationActionType action, String path) {
            this.agent = agent;
            this.action = action;
            this.path = path == null ? "/" : path;
        }

        @Override
        protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
//            log.info("complete ? ", decoder.isCompleted());
//            ByteBuffer buffer = ByteBuffer.allocate(1024);
//            decoder.read(buffer);
//            log.info("content {} received {},{}", new Object[]{buffer, decoder, ioctrl});
            log.info("event received");

            try {
                asyncReplicate(agent, action, path);
                log.info("replication request to agent {} sent ({} on {})", new Object[]{agent.getName(), action, path});
            } catch (AgentReplicationException e) {
                log.error("cannot replicate to agent {}, {}", agent.getName(), e);
            }

            super.onContentReceived(decoder, ioctrl);
        }

        @Override
        protected void onResponseReceived(HttpResponse response) throws IOException {
            log.info("response received {}", response);
            super.onResponseReceived(response);
        }
    }

    private void asyncReplicate(ReplicationAgent agent, ReplicationActionType action, String path) throws AgentReplicationException {
        agent.send(new ReplicationRequest(System.currentTimeMillis(), action, path));
    }

    private class EventBasedReplication implements Runnable {
        private final ReplicationAgent agent;
        private final ReplicationActionType actionType;
        private final String targetTransport;
        private final String path;

        public EventBasedReplication(ReplicationAgent agent, ReplicationActionType actionType, String path, String targetTransport) {
            this.agent = agent;
            this.actionType = actionType;
            this.targetTransport = targetTransport;
            this.path = path;
        }

        public void run() {
            try {
                ServiceReference[] serviceReferences = context.getServiceReferences(TransportHandler.class.getName(), targetTransport);

                log.info("reference transport for {} found {}", targetTransport, serviceReferences != null);

                if (serviceReferences != null && serviceReferences.length == 1) {

                    Object endpointsProperty = serviceReferences[0].getProperty("endpoints");
                    Object authenticationPropertiesProperty = serviceReferences[0].getProperty("authentication.properties");

                    log.info("endpoint prop: {} authentication properties prop: {}", endpointsProperty, authenticationPropertiesProperty);

                    String[] endpoints = (String[]) endpointsProperty;
                    Map<String, String> authenticationProperties = (Map<String, String>) authenticationPropertiesProperty;

                    log.info("endpoint {} props {}", endpoints, authenticationProperties);
                    // only works with HTTP
                    if (endpoints.length == 1 && endpoints[0].startsWith("http") && authenticationProperties != null) {
                        log.info("getting event queue URI");
                        URI eventEndpoint = URI.create(endpoints[0] + ReplicationConstants.SUFFIX_AGENT_QUEUE_EVENT);
                        String userName = authenticationProperties.get("user");
                        String password = authenticationProperties.get("password");

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
                                    new SSEResponseConsumer(agent, actionType, path), null);
                            requests.put(agent.getName(), futureResponse);
                            futureResponse.get();

                        } finally {
                            httpClient.close();
                        }
                        log.info("request finished");
                    }
                }
            } catch (Exception e) {
                log.error("cannot execute event based replication");
            }
        }
    }
}
