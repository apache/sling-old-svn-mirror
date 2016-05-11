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
package org.apache.sling.testing.clients.util;

import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

public class PortAllocator {

    private static Set<Integer> allocatedPorts;

    static {
        allocatedPorts = new HashSet<Integer>();
    }

    public Integer allocatePort() {
        while (true) {
            int port = tryAllocation();

            boolean portAdded = checkAndAddPort(port);

            if (portAdded) {
                return port;
            }
        }
    }

    private int tryAllocation() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (Exception e) {
            throw new RuntimeException("Can't allocate a port");
        }
    }

    private synchronized boolean checkAndAddPort(int port) {
        return allocatedPorts.add(port);
    }

}
