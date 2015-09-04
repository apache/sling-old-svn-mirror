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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Control listener.
 * This class listens for the startup of a launchpad instance.
 */
public class ControlListener implements Runnable {

    // command sent by the client to notify startup
    private static final String COMMAND_STARTED = "started";

    private static final String RESPONSE_OK = "ok";

    // The default interface to listen on
    private static final String DEFAULT_LISTEN_INTERFACE = "127.0.0.1";

    // The port to listen on
    private final int port;

    private volatile boolean started = false;

    private volatile boolean stopped = false;

    private volatile ServerSocket server;

    public ControlListener(final int p) {
        this.port = p;
        final Thread listener = new Thread(this);
        listener.setDaemon(true);
        listener.setName("Launchapd startup listener");
        listener.start();
    }

    public int getPort() {
        return this.port;
    }

    public boolean isStarted() {
        return this.started;
    }

    public void stop() {
        stopped = true;
        if ( server != null ) {
            try {
                server.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Implements the server thread receiving commands from clients and acting
     * upon them.
     */
    @Override
    public void run() {
        final InetSocketAddress socketAddress = getSocketAddress(this.port);
        try {
            server = new ServerSocket();
            server.bind(socketAddress);
        } catch (final IOException ioe) {
            return;
        }

        try {
            while (!stopped) {

                final Socket s = server.accept();

                try {
                    final String commandLine = readLine(s);
                    if (commandLine == null) {
                        final String msg = "ERR: missing command";
                        writeLine(s, msg);
                        continue;
                    }

                    final String command = commandLine;

                    if (COMMAND_STARTED.equals(command)) {
                        writeLine(s, RESPONSE_OK);
                        this.started = true;
                        this.stopped = true;
                        break;

                    } else {
                        final String msg = "ERR:" + command;
                        writeLine(s, msg);

                    }
                } finally {
                    try {
                        s.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        } catch (final IOException ioe) {
            // ignore
        } finally {
            try {
                server.close();
            } catch (final IOException ignore) {
                // ignore
            }
        }
    }

    private String readLine(final Socket socket) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(
            socket.getInputStream(), "UTF-8"));
        return br.readLine();
    }

    private void writeLine(final Socket socket, final String line) throws IOException {
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
            socket.getOutputStream(), "UTF-8"));
        bw.write(line);
        bw.write("\r\n");
        bw.flush();
    }

    private static InetSocketAddress getSocketAddress(final int port) {
        final String address = DEFAULT_LISTEN_INTERFACE;

        final InetSocketAddress addr = new InetSocketAddress(address, port);
        if (!addr.isUnresolved()) {
            return addr;
        }

        return null;
    }
}
