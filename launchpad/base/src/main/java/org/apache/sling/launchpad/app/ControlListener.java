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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

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

    // The default interface to listen on
    private static final String DEFAULT_LISTEN_INTERFACE = "127.0.0.1";

    // The default port to listen on and to connect to - we select it randomly
    private static final int DEFAULT_LISTEN_PORT = 0;

    // The reference to the Main class to shutdown on request
    private final Main slingMain;

    private final String listenSpec;

    private String secretKey;
    private InetSocketAddress socketAddress;

    /**
     * Creates an instance of this control support class.
     * <p>
     * The host (name or address) and port number of the socket is defined by
     * the <code>listenSpec</code> parameter. This parameter is defined as
     * <code>[ host ":" ] port</code>. If the parameter is empty or
     * <code>null</code> it defaults to <i>localhost:0</i>. If the host name
     * is missing it defaults to <i>localhost</i>.
     *
     * @param slingMain The Main class reference. This is only required if this
     *            instance is used for the server side to listen for remote stop
     *            commands. Otherwise this argument may be <code>null</code>.
     * @param listenSpec The specification for the host and port for the socket
     *            connection. See above for the format of this parameter.
     */
    ControlListener(final Main slingMain, final String listenSpec) {
        this.slingMain = slingMain;
        this.listenSpec = listenSpec; // socketAddress = this.getSocketAddress(listenSpec, selectNewPort);
    }

    /**
     * Implements the server side of the control connection starting a thread
     * listening on the host and port configured on setup of this instance.
     */
    boolean listen() {
        final File configFile = getConfigFile();
        if (configFile.canRead() && statusServer() == 0) {
            // server already running, fail
            Main.error("Sling already active in " + this.slingMain.getSlingHome(), null);
            return false;
        }
        configFile.delete();

        final Thread listener = new Thread(this);
        listener.setDaemon(true);
        listener.setName("Apache Sling Control Listener (inactive)");
        listener.start();
        return true;
    }

    /**
     * Implements the client side of the control connection sending the command
     * to shutdown Sling.
     */
    int shutdownServer() {
        return sendCommand(COMMAND_STOP);
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
        this.configure(false);

        ServerSocket server = null;
        try {
            server = new ServerSocket();
            server.bind(this.socketAddress);
            writePortToConfigFile(getConfigFile(),
                new InetSocketAddress(server.getInetAddress(), server.getLocalPort()), this.secretKey);
            Thread.currentThread().setName(
                "Apache Sling Control Listener@" + server.getInetAddress() + ":" + server.getLocalPort());
            Main.info("Apache Sling Control Listener started", null);
        } catch (final IOException ioe) {
            Main.error("Failed to start Apache Sling Control Listener", ioe);
            return;
        }

        long delay = 0;

        try {
            while (true) {

                final Socket s = server.accept();

                // delay processing after unsuccessfull attempts
                if (delay > 0) {
                    Main.info(s.getRemoteSocketAddress() + ": Delay: " + (delay / 1000), null);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                    }
                }

                try {
                    final String commandLine = readLine(s);
                    if (commandLine == null) {
                        final String msg = "ERR: missing command";
                        Main.info(s.getRemoteSocketAddress() + "<" + msg, null);
                        writeLine(s, msg);
                        continue;
                    }

                    final int blank = commandLine.indexOf(' ');
                    if (blank < 0) {
                        final String msg = "ERR: missing key";
                        Main.info(s.getRemoteSocketAddress() + "<" + msg, null);
                        writeLine(s, msg);
                        continue;
                    }

                    if (!secretKey.equals(commandLine.substring(0, blank))) {
                        final String msg = "ERR: wrong key";
                        Main.info(s.getRemoteSocketAddress() + "<" + msg, null);
                        writeLine(s, msg);
                        delay = (delay > 0) ? delay * 2 : 1000L;
                        continue;
                    }

                    final String command = commandLine.substring(blank + 1);
                    Main.info(s.getRemoteSocketAddress() + ">" + command, null);

                    if (COMMAND_STOP.equals(command)) {
                        slingMain.doStop();
                        Main.info(s.getRemoteSocketAddress() + "<" + RESPONSE_OK, null);
                        writeLine(s, RESPONSE_OK);
                        break;

                    } else if (COMMAND_STATUS.equals(command)) {
                        Main.info(s.getRemoteSocketAddress() + "<" + RESPONSE_OK, null);
                        writeLine(s, RESPONSE_OK);

                    } else {
                        final String msg = "ERR:" + command;
                        Main.info(s.getRemoteSocketAddress() + "<" + msg, null);
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
            Main.error("Failure reading from client", ioe);
        } finally {
            try {
                server.close();
            } catch (final IOException ignore) {
            }
        }

        getConfigFile().delete();

        // everything has stopped and when this thread terminates,
        // the VM should stop. If there are still some non-daemon threads
        // active, this will not happen, so we force this here ...
        Main.info("Apache Sling terminated, exiting Java VM", null);
        slingMain.terminateVM(0);
    }

    // ---------- socket support



    /**
     * Sends the given command to the server indicated by the configured
     * socket address and logs the reply.
     *
     * @param command The command to send
     *
     * @return A code indicating success of sending the command.
     */
    private int sendCommand(final String command) {
        if (configure(true)) {
            if (this.secretKey == null) {
                Main.info("Missing secret key to protect sending '" + command + "' to " + this.socketAddress, null);
                return 4; // LSB code for unknown status
            }

            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(this.socketAddress);
                writeLine(socket, this.secretKey + " " + command);
                final String result = readLine(socket);
                Main.info("Sent '" + command + "' to " + this.socketAddress + ": " + result, null);
                return 0; // LSB code for everything's fine
            } catch (final ConnectException ce) {
                Main.info("No Apache Sling running at " + this.socketAddress, null);
                return 3; // LSB code for programm not running
            } catch (final IOException ioe) {
                Main.error("Failed sending '" + command + "' to " + this.socketAddress, ioe);
                return 1; // LSB code for programm dead
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        Main.info("No socket address to send '" + command + "' to", null);
        return 4; // LSB code for unknown status
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

    /**
     * Read the port from the config file
     * @return The port or null
     */
    private boolean configure(final boolean fromConfigFile) {
        boolean result = false;
        if (fromConfigFile) {
            final File configFile = this.getConfigFile();
            if (configFile.canRead()) {
                FileReader fr = null;
                try {
                    fr = new FileReader(configFile);
                    final LineNumberReader lnr = new LineNumberReader(fr);
                    this.socketAddress = getSocketAddress(lnr.readLine());
                    this.secretKey = lnr.readLine();
                    result = true;
                } catch (final IOException ignore) {
                    // ignore
                } finally {
                    if (fr != null) {
                        try {
                            fr.close();
                        } catch (final IOException ignore) {
                        }
                    }
                }
            }
        } else {
            this.socketAddress = getSocketAddress(this.listenSpec);
            this.secretKey = generateKey();
            result = true;
        }

        return result;
    }

    private static String generateKey() {
        String keys = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz0123456789";
        int len = keys.length();
        Random r = new Random(System.currentTimeMillis() + 33 * System.nanoTime());
        char[] c = new char[32];
        for (int i = 0; i < c.length; i++) {
            c[i] = keys.charAt(r.nextInt(len));
        }
        return new String(c);
    }

    /**
     * Return the control port file
     */
    private File getConfigFile() {
        final File configDir = new File(this.slingMain.getSlingHome(), "conf");
        return new File(configDir, "controlport");
    }

    private static InetSocketAddress getSocketAddress(String listenSpec) {
        try {

            final String address;
            final int port;
            if (listenSpec == null) {
                address = DEFAULT_LISTEN_INTERFACE;
                port = DEFAULT_LISTEN_PORT;
            } else {
                final int colon = listenSpec.indexOf(':');
                if (colon < 0) {
                    address = DEFAULT_LISTEN_INTERFACE;
                    port = Integer.parseInt(listenSpec);
                } else {
                    address = listenSpec.substring(0, colon);
                    port = Integer.parseInt(listenSpec.substring(colon + 1));
                }
            }

            final InetSocketAddress addr = new InetSocketAddress(address, port);
            if (!addr.isUnresolved()) {
                return addr;
            }

            Main.error("Unknown host in '" + listenSpec, null);
        } catch (final NumberFormatException nfe) {
            Main.error("Cannot parse port number from '" + listenSpec + "'",
                null);
        }

        return null;
    }

    private static void writePortToConfigFile(final File configFile, final InetSocketAddress socketAddress,
            final String secretKey) {
        configFile.getParentFile().mkdirs();
        FileWriter fw = null;
        try {
            fw = new FileWriter(configFile);
            fw.write(socketAddress.getAddress().getHostAddress());
            fw.write(':');
            fw.write(String.valueOf(socketAddress.getPort()));
            fw.write('\n');
            fw.write(secretKey);
            fw.write('\n');
        } catch (final IOException ignore) {
            // ignore
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (final IOException ignore) {
                }
            }
        }
    }
}
