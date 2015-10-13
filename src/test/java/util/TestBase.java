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
package util;

import org.apache.http.HttpHost;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.localserver.SSLTestContexts;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.concurrent.TimeUnit;

public abstract class TestBase {
    public static final String ORIGIN = "TEST/1.1";
    protected static TestBase.ProtocolScheme scheme = TestBase.ProtocolScheme.http;
    protected static ServerBootstrap serverBootstrap;
    protected static HttpServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(5000).build();
        serverBootstrap = ServerBootstrap.bootstrap().setSocketConfig(socketConfig).setServerInfo("TEST/1.1");
        if(scheme.equals(TestBase.ProtocolScheme.https)) {
            serverBootstrap.setSslContext(SSLTestContexts.createServerSSLContext());
        }
    }

    @AfterClass
    public static void shutDown() throws Exception {
        if(server != null) {
            server.shutdown(-1, TimeUnit.SECONDS);
        }

    }

    public static HttpHost start() throws Exception {
        server = serverBootstrap.create();
        server.start();
        return new HttpHost("localhost", server.getLocalPort(), scheme.name());
    }

    public static enum ProtocolScheme {
        http,
        https;

        private ProtocolScheme() {
        }
    }
}
