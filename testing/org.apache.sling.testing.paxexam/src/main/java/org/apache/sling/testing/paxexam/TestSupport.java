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
package org.apache.sling.testing.paxexam;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Dictionary;

import javax.inject.Inject;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.keepCaches;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

public abstract class TestSupport {

    private final String workingDirectory = String.format("target/paxexam/%s", getClass().getSimpleName());

    @Inject
    protected ConfigurationAdmin configurationAdmin;

    protected String workingDirectory() {
        return workingDirectory;
    }

    protected synchronized int findFreePort() {
        try {
            final ServerSocket serverSocket = new ServerSocket(0);
            final int port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected int httpPort() throws IOException {
        final Dictionary<String, Object> properties = configurationAdmin.getConfiguration("org.apache.felix.http").getProperties();
        return Integer.parseInt(properties.get("org.osgi.service.http.port").toString());
    }

    protected Option baseConfiguration() {
        final String localRepository = System.getProperty("maven.repo.local", ""); // PAXEXAM-543
        return composite(
            systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
            when(localRepository.length() > 0).useOptions(
                systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepository)
            ),
            keepCaches(),
            CoreOptions.workingDirectory(workingDirectory()),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.testing.paxexam").versionAsInProject()
        );
    }

}
