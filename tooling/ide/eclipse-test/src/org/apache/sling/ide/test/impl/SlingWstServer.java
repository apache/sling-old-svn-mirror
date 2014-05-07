/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.test.impl;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.junit.rules.ExternalResource;

/**
 * The <tt>SlingWstServer</tt> sets up and tears down a Sling launchpad server
 *
 */
public class SlingWstServer extends ExternalResource {

    private IServer server;

    @Override
    protected void before() throws Throwable {
        int expectedServerCount = ServerCore.getServers().length + 1;
        int expectedRuntimeCount = ServerCore.getRuntimes().length + 1;

        IRuntimeType launchpadRuntime = null;
        for (IRuntimeType type : ServerCore.getRuntimeTypes()) {
            if ("org.apache.sling.ide.launchpadRuntimeType".equals(type.getId())) {
                launchpadRuntime = type;
                break;
            }
        }

        if (launchpadRuntime == null) {
            throw new IllegalArgumentException("No runtime of type 'org.apache.sling.ide.launchpadRuntimeType' found");
        }

        IRuntimeWorkingCopy rtwc = launchpadRuntime.createRuntime("temp.rt.id", new NullProgressMonitor());
        rtwc.save(true, new NullProgressMonitor());

        IServerType serverType = null;
        for (IServerType type : ServerCore.getServerTypes()) {
            System.out.println(type.getId());
            if ("org.apache.sling.ide.launchpadServer".equals(type.getId())) {
                serverType = type;
                break;
            }
        }

        if (serverType == null) {
            throw new IllegalArgumentException("No server type of type 'org.apache.sling.ide.launchpadServer' found");
        }

        IServerWorkingCopy wc = serverType.createServer("tmp.server.id", null, new NullProgressMonitor());
        // TODO - remove hardcoding
        wc.setHost("localhost");
        wc.setAttribute(ISlingLaunchpadServer.PROP_PORT, 8080);
        wc.setAttribute(ISlingLaunchpadServer.PROP_CONTEXT_PATH, "/");
        wc.setAttribute(ISlingLaunchpadServer.PROP_USERNAME, "admin");
        wc.setAttribute(ISlingLaunchpadServer.PROP_PASSWORD, "admin");
        wc.setAttribute("auto-publish-setting", ISlingLaunchpadServer.PUBLISH_STATE_RESOURCE_CHANGE);
        wc.setAttribute("auto-publish-time", 0);

        server = wc.save(true, new NullProgressMonitor());

        assertThat("server creation not registered - ServerCore.servers.length", ServerCore.getServers().length,
                equalTo(expectedServerCount));
        assertThat("runtime creation not registered - ServerCore.runtimes.length", ServerCore.getRuntimes().length,
                equalTo(expectedRuntimeCount));
    }

    @Override
    protected void after() {

        // TODO - destroy the server
    }

    public void waitForServerToStart() throws InterruptedException {
        waitForServerToStart(30, TimeUnit.SECONDS);
    }

    public void waitForServerToStart(long duration, TimeUnit timeUnit) throws InterruptedException {

        final CountDownLatch startLatch = new CountDownLatch(1);

        final IStatus[] statusHolder = new IStatus[1];

        server.start(ILaunchManager.RUN_MODE, new IOperationListener() {
            @Override
            public void done(IStatus result) {
                statusHolder[0] = result;
                startLatch.countDown();
            }
        });

        boolean success = startLatch.await(duration, timeUnit);
        assertThat("Server did not start in " + duration + " " + timeUnit, success, equalTo(true));
        assertThat("Unexpected IStatus when starting server", statusHolder[0], IStatusIsOk.isOk());
    }

    public IServer getServer() {
        return server;
    }

}
