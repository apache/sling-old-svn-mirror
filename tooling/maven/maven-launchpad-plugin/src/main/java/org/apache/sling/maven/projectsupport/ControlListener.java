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
package org.apache.sling.maven.projectsupport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.apache.maven.plugin.logging.Log;

/**
 * This class is adapted from org.apache.sling.launchpad.app.ControlListener.
 */
class ControlListener implements Runnable {
    // command sent by the client to cause Sling to shutdown
    static final String COMMAND_STOP = "stop";

    // command sent by the client to check for the status of the server
    static final String COMMAND_STATUS = "status";

    // the response sent by the server if the command executed successfully
    private static final String RESPONSE_OK = "OK";

    // The default port to listen on and to connect to
    private static final int DEFAULT_LISTEN_PORT = 63000;

    /** The mojo */
    private AbstractLaunchpadStartingMojo mojo;

    /** The log object */
    private final Log log;

    /** The socket address used for control communication */
    private final SocketAddress socketAddress;

    ControlListener(AbstractLaunchpadStartingMojo mojo, Log log, String host, int port) {
        this.mojo = mojo;
        this.log = log;
        this.socketAddress = getSocketAddress(host, port);
    }

    /**
     * Implements the server side of the control connection starting a thread
     * listening on the host and port configured on setup of this instance.
     */
    void listen() {
        if (socketAddress != null) {
            Thread listener = new Thread(this);
            listener.setDaemon(true);
            listener.setName("Sling Control Listener@" + socketAddress);
            listener.start();
        } else {
            log.info("No socket address to listen to");
        }
    }

    /**
     * Implements the client side of the control connection sending the command
     * to shutdown Sling.
     */
    void shutdownServer() {
        sendCommand(COMMAND_STOP);
    }

    /**
     * Implements the client side of the control connection sending the command
     * to check whether Sling is active.
     */
    void statusServer() {
        sendCommand(COMMAND_STATUS);
    }

    // ---------- Runnable interface

    /**
     * Implements the server thread receiving commands from clients and acting
     * upon them.
     */
    public void run() {
        ServerSocket server = null;
        try {
            server = new ServerSocket();
            server.bind(socketAddress);
            log.info("Sling Control Server started on " + socketAddress.toString());
        } catch (IOException ioe) {
            log.error("Failed to start Sling Control Server", ioe);
            return;
        }

        try {
            while (true) {

                Socket s = server.accept();
                try {
                    String command = readLine(s);
                    log.info(s.getRemoteSocketAddress() + ">" + command);

                    if (COMMAND_STOP.equals(command)) {
                        if (mojo != null) {
                            mojo.stopSling();
                        }
                        writeLine(s, RESPONSE_OK);

                        log.info("Sling shut down, stopping Sling.");
                        mojo.stopSling();

                    } else if (COMMAND_STATUS.equals(command)) {
                        writeLine(s, RESPONSE_OK);

                    } else {
                        writeLine(s, "ERR:" + command);

                    }
                } finally {
                    try {
                        s.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        } catch (IOException ioe) {
            log.error("Failure reading from client", ioe);
        } finally {
            try {
                server.close();
            } catch (IOException ignore) {
            }
        }
    }

    // ---------- socket support

    private SocketAddress getSocketAddress(String host, int port) {
        try {
            if (port == -1) {
                port = DEFAULT_LISTEN_PORT;
            }

            if (host != null) {
                return new InetSocketAddress(host, port);
            } else {

                return new InetSocketAddress(InetAddress.getLocalHost(), port);
            }
        } catch (UnknownHostException uhe) {
            log.error("Unknown host in '" + host + "': " + uhe.getMessage(), null);
        }

        return null;
    }

    private void sendCommand(String command) {
        if (socketAddress != null) {
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(socketAddress);
                writeLine(socket, command);
                String result = readLine(socket);
                log.info("Sent '" + command + "' to " + socketAddress + ": " + result, null);
            } catch (ConnectException ce) {
                log.info("No Sling running at " + socketAddress, null);
            } catch (IOException ioe) {
                log.error("Failed sending '" + command + "' to " + socketAddress, ioe);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        } else {
            log.info("No socket address to send '" + command + "' to", null);
        }
    }

    private String readLine(Socket socket) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        return br.readLine();
    }

    private void writeLine(Socket socket, String line) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        bw.write(line);
        bw.write("\r\n");
        bw.flush();
    }
}
