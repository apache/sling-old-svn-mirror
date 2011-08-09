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
package org.apache.sling.launchpad.app;

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

/**
 * The <code>ControlListener</code> class is a helper class for the {@link Main}
 * class to support in Sling standalone application process communication. This
 * class implements the client and server sides of a TCP/IP based communication
 * channel to control a running Sling application.
 * <p>
 * The server side listens for commands on a configurable host and port &endash;
 * <code>localhost:63000</code> by default &endash; supporting the following
 * commands:
 * <table>
 * <tr>
 * <th>Command</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td><code>status</code></td>
 * <td>Request status information. Currently only <i>OK</i> is sent back. If no
 * connection can be created to the server the client assumes Sling is not
 * running.</td>
 * </tr>
 * <tr>
 * <td><code>stop</code></td>
 * <td>Requests Sling to shutdown.</td>
 * </tr>
 * </table>
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

    // The reference to the Main class to shutdown on request
    private final Main slingMain;

    // The socket address used for control communication
    private final SocketAddress socketAddress;

    /**
     * Creates an instance of this control support class.
     * <p>
     * The host (name or address) and port number of the socket is defined by
     * the <code>listenSpec</code> parameter. This parameter is defined as
     * <code>[ host ":" ] port</code>. If the parameter is empty or
     * <code>null</code> it defaults to <i>localhost:63000</i>. If the host name
     * is missing it defaults to <i>localhost</i>.
     *
     * @param slingMain The Main class reference. This is only required if this
     *            instance is used for the server side to listen for remote stop
     *            commands. Otherwise this argument may be <code>null</code>.
     * @param listenSpec The specification for the host and port for the socket
     *            connection. See above for the format of this parameter.
     */
    ControlListener(Main slingMain, String listenSpec) {
        this.slingMain = slingMain;
        this.socketAddress = getSocketAddress(listenSpec);
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
            Main.info("No socket address to listen to", null);
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
    int statusServer() {
        return sendCommand(COMMAND_STATUS);
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
            Main.info("Sling Control Server started", null);
        } catch (IOException ioe) {
            Main.error("Failed to start Sling Control Server", ioe);
            return;
        }

        try {
            while (true) {

                Socket s = server.accept();
                try {
                    String command = readLine(s);
                    Main.info(s.getRemoteSocketAddress() + ">" + command, null);

                    if (COMMAND_STOP.equals(command)) {
                        slingMain.shutdown();
                        writeLine(s, RESPONSE_OK);

                        Main.info("Sling shut down, exiting Java VM", null);
                        System.exit(0);

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
            Main.error("Failure reading from client", ioe);
        } finally {
            try {
                server.close();
            } catch (IOException ignore) {
            }
        }
    }

    // ---------- socket support

    private SocketAddress getSocketAddress(String listenSpec) {
        try {
            if (listenSpec != null) {
                int colon = listenSpec.indexOf(':');
                if (colon < 0) {
                    return new InetSocketAddress(InetAddress.getLocalHost(),
                        Integer.parseInt(listenSpec));
                }
                return new InetSocketAddress(listenSpec.substring(0, colon),
                    Integer.parseInt(listenSpec.substring(colon + 1)));
            }

            return new InetSocketAddress(InetAddress.getLocalHost(),
                DEFAULT_LISTEN_PORT);
        } catch (NumberFormatException nfe) {
            Main.error("Cannot parse port number from '" + listenSpec + "'",
                null);
        } catch (UnknownHostException uhe) {
            Main.error("Unknown host in '" + listenSpec + "': "
                + uhe.getMessage(), null);
        }

        return null;
    }

    /**
     * Sends the given command to the server indicated by the configured
     * socket address and logs the reply.
     *
     * @param command The command to send
     *
     * @return A code indicating success of sending the command.
     */
    private int sendCommand(String command) {
        if (socketAddress != null) {
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(socketAddress);
                writeLine(socket, command);
                String result = readLine(socket);
                Main.info("Sent '" + command + "' to " + socketAddress + ": "
                    + result, null);
                return 0; // LSB code for everything's fine
            } catch (ConnectException ce) {
                Main.info("No Sling running at " + socketAddress, null);
                return 3; // LSB code for programm not running
            } catch (IOException ioe) {
                Main.error("Failed sending '" + command + "' to "
                    + socketAddress, ioe);
                return 1; // LSB code for programm dead
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        } else {
            Main.info("No socket address to send '" + command + "' to", null);
            return 4; // LSB code for unknown status
        }
    }

    private String readLine(Socket socket) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(
            socket.getInputStream(), "UTF-8"));
        return br.readLine();
    }

    private void writeLine(Socket socket, String line) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
            socket.getOutputStream(), "UTF-8"));
        bw.write(line);
        bw.write("\r\n");
        bw.flush();
    }
}
