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
 */package org.apache.sling.maven.slingstart.launcher;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Launcher implements LauncherMBean {

    private final int listenerPort;

    public Launcher(final int listenerPort) {
        this.listenerPort = listenerPort;
    }

    @Override
    public void startupFinished() {
        final List<String> hosts = new ArrayList<String>();
        hosts.add("localhost");
        hosts.add("127.0.0.1");

        boolean done = false;
        int index = 0;
        while ( !done && index < hosts.size() ) {
            final String hostName = hosts.get(index);
            final int twoMinutes = 2 * 60 * 1000;

            Socket clientSocket = null;
            DataOutputStream out = null;
            BufferedReader in = null;
            try {
                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(hostName, listenerPort), twoMinutes);
                // without that, read() call on the InputStream associated with this Socket is infinite
                clientSocket.setSoTimeout(twoMinutes);

                out = new DataOutputStream(clientSocket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out.writeBytes("started\n");
                in.readLine();
                done = true;
            } catch (final Throwable ignore) {
                // catch Throwable because InetSocketAddress and Socket#connect throws unchecked exceptions
                // we ignore this for now
            } finally {
                if ( in != null ) {
                    try {
                        in.close();
                    } catch ( final IOException ioe) {
                        // ignore
                    }
                }
                if ( out != null ) {
                    try {
                        out.close();
                    } catch ( final IOException ioe) {
                        // ignore
                    }
                }
                if ( clientSocket != null ) {
                    try {
                        clientSocket.close();
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }
            index++;
        }
    }

    @Override
    public void startupProgress(Float ratio) {
        // nothing to do
    }
}
