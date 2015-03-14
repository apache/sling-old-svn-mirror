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
package org.apache.sling.maven.slingstart.run;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Simple helper class to find a new port.
 */
public class PortHelper {

    private static final Set<Integer> USED_PORTS = new HashSet<Integer>();

    public static synchronized int getNextAvailablePort()
            throws MojoExecutionException {
        int unusedPort = 0;
        do {
            try {
                final ServerSocket socket = new ServerSocket( 0 );
                unusedPort = socket.getLocalPort();
                socket.close();
            } catch ( final IOException e ) {
                throw new MojoExecutionException( "Error getting an available port from system", e );
            }
        } while ( USED_PORTS.contains(unusedPort));
        USED_PORTS.add(unusedPort);

        return unusedPort;
    }
}
