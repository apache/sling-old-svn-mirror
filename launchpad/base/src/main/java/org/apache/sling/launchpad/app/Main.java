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
package org.apache.sling.launchpad.app;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.launchpad.base.shared.Launcher;
import org.apache.sling.launchpad.base.shared.Loader;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.apache.sling.launchpad.base.shared.SharedConstants;

/**
 * The <code>Main</code> is the externally visible Standalone Java Application
 * launcher for Sling. Please refer to the full description <i>The Sling
 * Launchpad</i> on the Sling Wiki for a full description of this class.
 * <p>
 * Logging goes to standard output for informational messages and to standard
 * error for error messages.
 * <p>
 * This class goes into the secondary artifact with the classifier <i>app</i> to
 * be used as the main class when starting the Java Application.
 *
 * @see <a href="http://cwiki.apache.org/SLING/the-sling-launchpad.html">The
 *      Sling Launchpad</a>
 */
public class Main extends Thread implements Notifiable {

    // The name of the environment variable to consult to find out
    // about sling.home
    private static final String ENV_SLING_HOME = "SLING_HOME";

    public static void main(String[] args) {
        new Main(args);
    }

    private final Map<String, String> commandLineArgs;

    private final String slingHome;

    private final Loader loader;

    private Launcher sling;

    private Main(String[] args) {

        // set the thread name
        super("Apache Sling Terminator");

        this.commandLineArgs = parseCommandLine(args);

        // support usage first
        doHelp();

        // check for control commands (might exit)
        doControlCommand();

        // sling.home from the command line or system properties, else default
        this.slingHome = getSlingHome(commandLineArgs);
        File slingHomeFile = new File(slingHome);
        if (slingHomeFile.isAbsolute()) {
            info("Starting Apache Sling in " + slingHome, null);
        } else {
            info("Starting Apache Sling in " + slingHome + " ("
                + slingHomeFile.getAbsolutePath() + ")", null);
        }

        // The Loader helper
        Loader loaderTmp = null;
        try {
            loaderTmp = new Loader(slingHome) {
                @Override
                protected void info(String msg) {
                    Main.info(msg, null);
                }
            };
        } catch (IllegalArgumentException iae) {
            startupFailure(iae.getMessage(), null);
        }
        this.loader = loaderTmp;

        Runtime.getRuntime().addShutdownHook(this);

        // ensure up-to-date launcher jar
        startSling(getClass().getResource(
            SharedConstants.DEFAULT_SLING_LAUNCHER_JAR));
    }

    // ---------- Shutdown support for control listener and shutdown hook

    void shutdown() {
        // remove the shutdown hook, will fail if called from the
        // shutdown hook itself. Otherwise this prevents shutdown
        // from being called again
        try {
            Runtime.getRuntime().removeShutdownHook(this);
        } catch (Throwable t) {
            // don't care for problems removing the hook
        }

        // now really shutdown sling
        if (sling != null) {
            info("Stopping Apache Sling", null);
            sling.stop();
        }
    }

    // ---------- Notifiable interface

    /**
     * The framework has been stopped by calling the <code>Bundle.stop()</code>
     * on the system bundle. This actually terminates the Sling Standalone
     * application.
     */
    public void stopped() {
        /**
         * This method is called if the framework is stopped from within by
         * calling stop on the system bundle or if the framework is stopped
         * because the VM is going down and the shutdown hook has initated the
         * shutdown In any case we ensure the reference to the framework is
         * removed and remove the shutdown hook (but don't care if that fails).
         */

        info("Apache Sling has been stopped", null);

        // clear the reference to the framework
        sling = null;

        // remove the shutdown hook, the framework has terminated and
        // we do not need to do anything else
        try {
            Runtime.getRuntime().removeShutdownHook(this);
        } catch (Throwable t) {
            // don't care for problems removing the hook
        }
    }

    /**
     * The framework has been stopped with the intent to be restarted by calling
     * either of the <code>Bundle.update</code> methods on the system bundle.
     * <p>
     * If an <code>InputStream</code> was provided, this has been copied to a
     * temporary file, which will be used in place of the existing launcher jar
     * file.
     *
     * @param updateFile The temporary file to replace the existing launcher jar
     *            file. If <code>null</code> the existing launcher jar will be
     *            used again.
     */
    public void updated(File updateFile) {

        // clear the reference to the framework
        sling = null;

        // ensure we have a VM as clean as possible
        loader.cleanupVM();

        if (updateFile == null) {

            info("Restarting Framework and Apache Sling", null);
            startSling(null);

        } else {

            info("Restarting Framework with update from " + updateFile, null);
            try {
                startSling(updateFile.toURI().toURL());
            } catch (MalformedURLException mue) {
                error("Cannot get URL for file " + updateFile, mue);
            } finally {
                updateFile.delete();
            }

        }
    }

    // --------- Thread

    /**
     * Called when the Java VM is being terminiated, for example because the
     * KILL signal has been sent to the process. This method calls stop on the
     * launched Sling instance to terminate the framework before returning.
     */
    @Override
    public void run() {
        info("Java VM is shutting down", null);
        shutdown();
    }

    // ---------- internal

    private void startSling(URL launcherJar) {
        if (launcherJar != null) {
            try {
                info("Checking launcher JAR in folder " + slingHome, null);
                loader.installLauncherJar(launcherJar);
            } catch (IOException ioe) {
                startupFailure("Failed installing " + launcherJar, ioe);
            }
        } else {
            info("No Launcher JAR to install", null);
        }

        Object object = null;
        try {
            object = loader.loadLauncher(SharedConstants.DEFAULT_SLING_MAIN);
        } catch (IllegalArgumentException iae) {
            startupFailure("Failed loading Sling class "
                + SharedConstants.DEFAULT_SLING_MAIN, iae);
        }

        if (object instanceof Launcher) {

            // configure the launcher
            Launcher sling = (Launcher) object;
            sling.setNotifiable(this);
            sling.setCommandLine(commandLineArgs);
            sling.setSlingHome(slingHome);

            // launch it
            info("Starting launcher ...", null);
            if (sling.start()) {
                info("Startup completed", null);
                this.sling = sling;
            } else {
                error("There was a problem launching Apache Sling", null);
            }
        }
    }

    /**
     * Parses the command line arguments into a map of strings indexed by
     * strings. This method suppports single character option names only at the
     * moment. Each pair of an option name and its value is stored into the
     * map. If a single dash '-' character is encountered the rest of the command
     * line are interpreted as option names and are stored in the map unmodified
     * as entries with the same key and value.
     * <table>
     * <tr><th>Command Line</th><th>Mapping</th></tr>
     * <tr><td>x</td><td>x -> x</td></tr>
     * <tr><td>-y z</td><td>y -> z</td></tr>
     * <tr><td>-yz</td><td>y -> z</td></tr>
     * <tr><td>-y -z</td><td>y -> y, z -> z</td></tr>
     * <tr><td>-y x - -z a</td><td>y -> x, -z -> -z, a -> a</td></tr>
     * </table>
     *
     * @param args The command line to parse
     *
     * @return The map of command line options and their values
     */
    static Map<String, String> parseCommandLine(String[] args) {
        Map<String, String> commandLine = new HashMap<String, String>();
        boolean readUnparsed = false;
        for (int argc = 0; args != null && argc < args.length; argc++) {
            String arg = args[argc];

            if (readUnparsed) {
                commandLine.put(arg, arg);
            } else if (arg.startsWith("-")) {
                if (arg.length() == 1) {
                   readUnparsed = true;
                } else {
                    String key = String.valueOf(arg.charAt(1));
                    if (arg.length() > 2) {
                        commandLine.put(key, arg.substring(2));
                    } else {
                        argc++;
                        if (argc < args.length
                            && (args[argc].equals("-") || !args[argc].startsWith("-"))) {
                            commandLine.put(key, args[argc]);
                        } else {
                            commandLine.put(key, key);
                            argc--;
                        }
                    }
                }
            } else {
                commandLine.put(arg, arg);
            }
        }
        return commandLine;
    }

    /**
     * Define the sling.home parameter implementing the algorithme defined on
     * the wiki page to find the setting according to this algorithm:
     * <ol>
     * <li>Command line option <code>-c</code></li>
     * <li>System property <code>sling.home</code></li>
     * <li>Environment variable <code>SLING_HOME</code></li>
     * <li>Default value <code>sling</code></li>
     * </ol>
     *
     * @param args The command line arguments
     * @return The value to use for sling.home
     */
    private static String getSlingHome(Map<String, String> commandLine) {
        String source = null;

        String slingHome = commandLine.get("c");
        if (slingHome != null) {

            source = "command line";

        } else {

            slingHome = System.getProperty(SharedConstants.SLING_HOME);
            if (slingHome != null) {

                source = "system property sling.home";

            } else {

                slingHome = System.getenv(ENV_SLING_HOME);
                if (slingHome != null) {

                    source = "environment variable SLING_HOME";

                } else {

                    source = "default";
                    slingHome = SharedConstants.SLING_HOME_DEFAULT;

                }
            }
        }

        info("Setting sling.home=" + slingHome + " (" + source + ")", null);
        return slingHome;
    }

    private void startupFailure(String message, Throwable cause) {
        error("Launcher JAR access failure: " + message, cause);
        error("Shutting Down", null);
        System.exit(1);
    }

    // ---------- logging

    // emit an informational message to standard out
    static void info(String message, Throwable t) {
        log(System.out, "*INFO*", message, t);
    }

    // emit an error message to standard err
    static void error(String message, Throwable t) {
        log(System.err, "*ERROR*", message, t);
    }

    private static final DateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS ");

    // helper method to format the message on the correct output channel
    // the throwable if not-null is also prefixed line by line with the prefix
    private static void log(PrintStream out, String prefix, String message,
            Throwable t) {

        final StringBuilder linePrefixBuilder = new StringBuilder();
        synchronized (fmt) {
            linePrefixBuilder.append(fmt.format(new Date()));
        }
        linePrefixBuilder.append(prefix);
        linePrefixBuilder.append(" [");
        linePrefixBuilder.append(Thread.currentThread().getName());
        linePrefixBuilder.append("] ");
        final String linePrefix = linePrefixBuilder.toString();

        out.print(linePrefix);
        out.println(message);
        if (t != null) {
            t.printStackTrace(new PrintStream(out) {
                @Override
                public void println(String x) {
                    synchronized (this) {
                        print(linePrefix);
                        super.println(x);
                        flush();
                    }
                }
            });
        }
    }

    /** prints a simple usage plus optional error message and exists */
    private void doHelp() {
        if (commandLineArgs.remove("h") != null) {
            System.out.println("usage: "
                + Main.class.getName()
                + " [ start | stop | status ] [ -j adr ] [ -l loglevel ] [ -f logfile ] [ -c slinghome ] [ -a address ] [ -p port ] [ -h ]");

            System.out.println("    start         listen for control connection (uses -j)");
            System.out.println("    stop          terminate running Apache Sling (uses -j)");
            System.out.println("    status        check whether Apache Sling is running (uses -j)");
            System.out.println("    -j adr        host and port to use for control connection in the format '[host:]port' (default localhost:63000)");
            System.out.println("    -l loglevel   the initial loglevel (0..4, FATAL, ERROR, WARN, INFO, DEBUG)");
            System.out.println("    -f logfile    the log file, \"-\" for stdout (default logs/error.log)");
            System.out.println("    -c slinghome  the sling context directory (default sling)");
            System.out.println("    -a address    the interfact to bind to (use 0.0.0.0 for any) (not supported yet)");
            System.out.println("    -p port       the port to listen to (default 8080)");
            System.out.println("    -h            prints this usage message");

            System.exit(0);
        }
    }

    private void doControlCommand() {
        String commandSocketSpec = commandLineArgs.remove("j");
        if ("j".equals(commandSocketSpec)) {
            commandSocketSpec = null;
        }

        ControlListener sl = new ControlListener(this, commandSocketSpec);
        if (commandLineArgs.remove(ControlListener.COMMAND_STOP) != null) {
            sl.shutdownServer();
            System.exit(0);
        } else if (commandLineArgs.remove(ControlListener.COMMAND_STATUS) != null) {
            final int status = sl.statusServer();
            System.exit(status);
        } else if (commandLineArgs.remove("start") != null) {
            sl.listen();
        }
    }
}
