/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.tools.serversetup;

import java.util.Properties;

import org.apache.sling.testing.tools.jarexec.JarExecutor;
import org.apache.sling.testing.tools.jarexec.JarExecutor.ExecutorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SetupPhase that uses a JarExecutor to start
 *  a runnable jar, and stop it at system shutdown
 *  if our SetupServer wants that.
 */
public class StartRunnableJarPhase implements SetupPhase {

    public static final String TEST_SERVER_HOSTNAME = "test.server.hostname";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String id;
    private final String description;
    private final JarExecutor executor;
    
    public StartRunnableJarPhase(final ServerSetup owner, String id, String description, Properties config) throws ExecutorException {
        this.id = id;
        this.description = description;
        executor = new JarExecutor(config);

        String hostname = config.getProperty(TEST_SERVER_HOSTNAME);
        if(hostname == null) {
            hostname = "localhost";
        }
        final String url = "http://" + hostname + ":" + executor.getServerPort();
        log.info("Server base URL={}", url);
        owner.getContext().put(ServerSetup.SERVER_BASE_URL, url);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + id + ") " + description; 
    }
    
    public void run(ServerSetup owner) throws Exception {
        executor.start();
    }

    public boolean isStartupPhase() {
        return true;
    }

    public String getId() {
        return id;
    }
    
    /** Return a SetupPhase that kills the process started by this phase */
    public SetupPhase getKillPhase(final String id) {
        return new SetupPhase() {
            public void run(ServerSetup owner) throws Exception {
                executor.stop();
            }

            public boolean isStartupPhase() {
                // This is not a shutdown phase, it's meant to
                // use during startup to forcibly kill an instance
                return true;
            }

            @Override
            public String toString() {
                return "Kill the process started by " + StartRunnableJarPhase.this;
            }

            public String getId() {
                return id;
            }
        };
    }
}
