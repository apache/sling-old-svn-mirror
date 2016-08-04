/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi.client.test.util;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.localserver.SSLTestContexts;
import org.junit.rules.ExternalResource;

/** JUnit Rule that starts an HTTP server */
public class HttpServerRule extends ExternalResource {
    public static final String ORIGIN = "TEST/1.1";
    private HttpServer server;
    private HttpHost host;
    private URI uri;

    protected ServerBootstrap serverBootstrap; 
    
    public static enum ProtocolScheme {
        http,
        https;
        private ProtocolScheme() {
        }
    }
    
    protected final ProtocolScheme protocolScheme;

    public HttpServerRule() {
        this(ProtocolScheme.http);
    }
    
    public HttpServerRule(ProtocolScheme protocolScheme) {
        this.protocolScheme = protocolScheme;
    }
    
    @Override
    protected void after() {
        server.shutdown(-1, TimeUnit.SECONDS);
    }

    @Override
    protected void before() throws Throwable {
        final SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(5000).build();
        serverBootstrap = ServerBootstrap.bootstrap().setSocketConfig(socketConfig).setServerInfo(ORIGIN);
        if(ProtocolScheme.https.equals(protocolScheme)) {
            serverBootstrap.setSslContext(SSLTestContexts.createServerSSLContext());
        }
        registerHandlers();
        server = serverBootstrap.create();
        server.start();
        host = new HttpHost("127.0.0.1", server.getLocalPort(), protocolScheme.name());
        uri = URIUtils.rewriteURI(new URI("/"), host);
    }
    
    protected void registerHandlers() throws IOException {
    }
    
    public URI getURI() {
        return uri;
    }
}
