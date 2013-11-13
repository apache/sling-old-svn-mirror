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
import java.util.Map.Entry;

import org.apache.sling.launchpad.base.shared.Launcher;
import org.apache.sling.launchpad.base.shared.Loader;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.apache.sling.launchpad.base.shared.SharedConstants;

/**
 * The <code>Main</code> is the externally visible Standalone Java Application
 * launcher for Sling. Please refer to the full description <i>The Sling
 * Launchpad</i> on the Sling Web Site for a full description of this class.
 * <p>
 * Logging goes to standard output for informational messages and to standard
 * error for error messages.
 * <p>
 * This class goes into the secondary artifact with the classifier <i>app</i> to
 * be used as the main class when starting the Java Application.
 *
 * @see <a href="http://sling.apache.org/site/the-sling-launchpad.html">The
 *      Sling Launchpad</a>
 */
public class Main {

    // The name of the environment variable to consult to find out
    // about sling.home
    private static final String ENV_SLING_HOME = "SLING_HOME";

    /**
     * The name of the configuration property indicating the
     * {@link ControlAction} to be taken in the {@link #doControlAction()}
     * method.
     */
    protected static final String PROP_CONTROL_ACTION = "sling.control.action";

    /**
     * The name of the configuration property indicating the socket to use for
     * the control connection. The value of this property is either just a port
     * number (in which case the host is assumed to be <code>localhost</code>)
     * or a host name (or IP address) and port number separated by a colon.
     */
    protected static final String PROP_CONTROL_SOCKET = "sling.control.socket";

    /** The Sling configuration property name setting the initial log level */
    private static final String PROP_LOG_LEVEL = "org.apache.sling.commons.log.level";

    /** The Sling configuration property name setting the initial log file */
    private static final String PROP_LOG_FILE = "org.apache.sling.commons.log.file";

    /**
     * The configuration property setting the port on which the HTTP service
     * listens
     */
    private static final String PROP_PORT = "org.osgi.service.http.port";

    /**
     * The configuration property setting the context path where the HTTP service
     * mounts itself.
     */
    private static final String PROP_CONTEXT_PATH = "org.apache.felix.http.context_path";

    /**
     * Host name or IP Address of the interface to listen on.
     */
    private static final String PROP_HOST = "org.apache.felix.http.host";

    /**
     * Name of the configuration property (or system property) indicating
     * whether the shutdown hook should be installed or not. If this property is
     * not set or set to {@code true} (case insensitive), the shutdown hook
     * properly shutting down the framework is installed on startup. Otherwise,
     * if this property is set to any value other than {@code true} (case
     * insensitive) the shutdown hook is not installed.
     * <p>
     * The respective command line option is {@code -n}.
     */
    private static final String PROP_SHUTDOWN_HOOK = "sling.shutdown.hook";

    /**
     * The main entry point to the Sling Launcher Standalone Java Application.
     * This method is generally only called by the Java VM to launch Sling.
     *
     * @param args The command line arguments supplied when starting the Sling
     *            Launcher through the Java VM.
     */
    public static void main(final String[] args) {
        final Map<String, String> rawArgs = parseCommandLine(args);

        // support usage first
        if (doHelp(rawArgs)) {
            System.exit(0);
        }

        final Map<String, String> props = convertCommandLineArgs(rawArgs);
        if (props == null) {
            System.exit(1);
        }

        final Main main = new Main(props);

        // check for control commands
        int rc = main.doControlAction();
        if (rc >= 0) {
            main.terminateVM(rc);
        }

        // finally start Sling
        if (!main.doStart()) {
            error("Failed to start Sling; terminating", null);
            main.terminateVM(1);
        }
    }

    /**
     * The map of command line arguments where the keys are the actual
     * property names as known to the OSGi Framework and its installed
     * bundles.
     */
    private final Map<String, String> commandLineArgs;

    /**
     * Whether to install the shutdown hook.
     *
     * @see #PROP_SHUTDOWN_HOOK
     * @see #installShutdownHook(Map)
     * @see #addShutdownHook()
     */
    private boolean installShutdownHook;

    /**
     * The shutdown hook installed into the Java VM after Sling has been
     * started. The hook is removed again when Sling is being shut down
     * or the {@link Notified notifier} is notified of the framework shutdown.
     *
     * @see #addShutdownHook()
     * @see #removeShutdownHook()
     */
    private Thread shutdownHook;

    /**
     * The absolute path to the home directory of the launched Sling
     * application. This corresponds to the value of the <code>sling.home</code>
     * framework property.
     *
     * @see #getSlingHome(Map)
     */
    private final String slingHome;

    /**
     * The {@link Loader} class used to create the Framework class loader and
     * to launch the framework.
     */
    private Loader loader;

    /**
     * The actual launcher accessed through the {@link #loader} to launch
     * the OSGi Framework.
     */
    private Launcher sling;

    /**
     * Flag to indicate if Sling has already been started.
     */
    private boolean started = false;

    /**
     * Creates an instance of this main loader class. The provided arguments are
     * used to configure the OSGi framework being launched with the
     * {@link #startSling(URL)} method.
     *
     * @param args The map of configuration properties to be supplied to the
     *            OSGi framework. The keys in this map are assumed to be usefull
     *            without translation to the launcher and the OSGi Framework. If
     *            this parameter is <code>null</code> and empty map without
     *            configuration is assumed.
     */
    protected Main(Map<String, String> args) {
        this.commandLineArgs = (args == null)
                ? new HashMap<String, String>()
                : args;

        this.installShutdownHook = installShutdownHook(this.commandLineArgs);

        // sling.home from the command line or system properties, else default
        String home = getSlingHome(commandLineArgs);
        final File slingHomeFile = new File(home);
        if (!slingHomeFile.isAbsolute()) {
            home = slingHomeFile.getAbsolutePath();
        }
        this.slingHome = home;
    }

    /**
     * After instantiating this class, this method may be called to help with
     * the communication with a running Sling instance. To setup this
     * communication the configuration properties supplied to the constructor
     * are evaluated as follows:
     * <p>
     * <table>
     * <tr>
     * <td><code>{@value #PROP_CONTROL_SOCKET}</code></td>
     * <td>Specifies the socket to use for the control connection. This
     * specification is of the form <i>host:port</i> where the host can be a
     * host name or IP Address and may be omitted (along with the separating
     * colon) and port is just the numberic port number at which to listen. The
     * default is <i>localhost:63000</i>. It is suggested to not use an
     * externally accessible interface for security reasons because there is no
     * added security on this control channel for now.</td>
     * </tr>
     * <tr>
     * <td><code>{@value #PROP_CONTROL_ACTION}</code></td>
     * <td>The actual action to execute:
     * <ul>
     * <b>start</b> -- Start the listener on the configured socket and expect
     * commands there. This action is useful only when launching the Sling
     * application since this action helps manage a running system.
     * </ul>
     * <ul>
     * <b>stop</b> -- Connects to the listener running on the configured socket
     * and send the command to terminate the Sling Application. If this command
     * is used, it is expected the Sling Application will not start.
     * </ul>
     * <ul>
     * <b>status</b> -- Connects to the listener running on the configured
     * socket and query about its status. If this command is used, it is
     * expected the Sling Application will not start.
     * </ul>
     * </td>
     * </tr>
     * </table>
     * <p>
     * After this method has executed the <code>j</code> and
     * {@link #PROP_CONTROL_ACTION} properties have been removed from the
     * configuration properties.
     * <p>
     * While the {@link #doStart()} and {@link #doStop()} methods may be called
     * multiple times this method should only be called once after creating this
     * class's instance.
     *
     * @return An code indicating whether the Java VM is expected to be
     *         terminated or not. If <code>-1</code> is returned, the VM should
     *         continue as intended, maybe starting the Sling Application. This
     *         code is returned if the start action (or no action at all) is
     *         supplied. Otherwise the VM should terminate with the returned
     *         code as its exit code. For the stop action, this will be zero.
     *         For the status action, this will be a LSB compliant code for
     *         daemon status check: 0 (application running), 1 (Programm Dead),
     *         3 (Programm Not Running), 4 (Unknown Problem).
     * @see <a
     *      href="http://refspecs.linuxbase.org/LSB_3.1.0/LSB-Core-generic/LSB-Core-generic/iniscrptact.html">Init Script Actions</a>
     *      for a definition of the LSB status codes
     */
    protected int doControlAction() {
        final ControlAction action = getControlAction();
        if (action != null) {
            final ControlListener sl = new ControlListener(this,
                commandLineArgs.remove(PROP_CONTROL_SOCKET));
            switch (action) {
                case START:
                    if (!sl.listen()) {
                        // assume service already running
                        return 0;
                    }
                    break;
                case STATUS:
                    return sl.statusServer();
                case STOP:
                    return sl.shutdownServer();
            }
        }

        return -1;
    }

    /**
     * Terminates the VM which was started by calling the
     * {@link #main(String[])} method of this class using the
     * <code>status</code> value as the application exit status code.
     * <p>
     * This method does not return.
     *
     * @param status The application status exit code.
     */
    // default accessor to enable overwriting for unit tests
    void terminateVM(final int status) {
        System.exit(status);
    }

    private ControlAction getControlAction() {
        Object action = this.commandLineArgs.remove(PROP_CONTROL_ACTION);
        if (action != null) {
            if (action instanceof ControlAction) {
                return (ControlAction) action;
            }

            try {
                return ControlAction.valueOf(action.toString().toUpperCase());
            } catch (IllegalArgumentException iae) {
                error("Illegal control action value: " + action, null);
            }
        }
        return null;
    }

    /**
     * Starts the application with the configuration supplied with the
     * configuration properties when this instance has been created.
     * <p>
     * Calling this method multiple times before calling {@link #doStop()} will
     * cause a message to be printed and <code>true</code> being returned.
     *
     * @return <code>true</code> if startup was successfull or the application
     *         is considered to be started already. Otherwise an error message
     *         has been logged and <code>false</code> is returned.
     */
    protected boolean doStart() {
        // ensure up-to-date launcher jar
        return doStart(getClass().getResource(
            SharedConstants.DEFAULT_SLING_LAUNCHER_JAR));
    }

    protected boolean doStart(final URL launcherJar) {

        // prevent duplicate start
        if ( this.started) {
            info("Apache Sling has already been started", null);
            return true;
        }

        info("Starting Apache Sling in " + slingHome, null);
        this.started = true;

        Loader loaderTmp = null;
        try {
            final File launchpadHome = getLaunchpadHome(slingHome,
                commandLineArgs);
            loaderTmp = new Loader(launchpadHome) {
                @Override
                protected void info(String msg) {
                    Main.info(msg, null);
                }
            };
        } catch (IllegalArgumentException iae) {
            error(
                "Cannot launch: Launchpad folder cannot be used: "
                    + iae.getMessage(), null);
            return false;
        }
        this.loader = loaderTmp;

        if (launcherJar != null) {
            try {
                loader.installLauncherJar(launcherJar);
            } catch (IOException ioe) {
                error("Cannot launch: Cannot install " + launcherJar
                    + " for use", ioe);
                return false;
            }
        } else {
            info("No Launcher JAR to install", null);
        }

        Object object = null;
        try {
            object = loader.loadLauncher(SharedConstants.DEFAULT_SLING_MAIN);
        } catch (IllegalArgumentException iae) {
            error("Cannot launch: Failed loading Sling class "
                + SharedConstants.DEFAULT_SLING_MAIN, iae);
            return false;
        }

        if (object instanceof Launcher) {

            // configure the launcher
            Launcher sling = (Launcher) object;
            sling.setNotifiable(new Notified());
            sling.setCommandLine(commandLineArgs);
            sling.setSlingHome(slingHome);

            // launch it
            info("Starting launcher ...", null);
            if (sling.start()) {
                info("Startup completed", null);
                this.sling = sling;
                addShutdownHook();
                return true;
            }

            error("Cannot launch: Launcher.start() returned false", null);

        } else {

            error("Cannot launch: Class " + SharedConstants.DEFAULT_SLING_MAIN + " is not a Launcher class", null);

        }

        return false;
    }

    /**
     * Maybe called by the application to cause the Sling Application to
     * properly terminate by stopping the OSGi Framework.
     * <p>
     * After calling this method the Sling Application can be started again
     * by calling the {@link #doStart()} method.
     * <p>
     * Calling this method multiple times without calling the {@link #doStart()}
     * method in between has no effect after the Sling Application has been
     * terminated.
     */
    protected void doStop() {
        removeShutdownHook();

        // now really shutdown sling
        if (this.sling != null) {
            info("Stopping Apache Sling", null);
            this.sling.stop();
            this.sling = null;
        }

        // clean and VM caches
        if (this.loader != null) {
            this.loader.cleanupVM();
            this.loader = null;
        }

        // further cleanup
        this.started = false;
    }

    private void addShutdownHook() {
        if (this.installShutdownHook && this.shutdownHook == null) {
            this.shutdownHook = new Thread(new ShutdownHook(), "Apache Sling Terminator");
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }

    private void removeShutdownHook() {
        // remove the shutdown hook, will fail if called from the
        // shutdown hook itself. Otherwise this prevents shutdown
        // from being called again
        Thread shutdownHook = this.shutdownHook;
        this.shutdownHook = null;

        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (Throwable t) {
                // don't care for problems removing the hook
            }
        }
    }

    /**
     * Define the sling.home parameter implementing the algorithme defined on
     * the wiki page to find the setting according to this algorithm:
     * <ol>
     * <li>Configuration property <code>sling.home</code></li>
     * <li>System property <code>sling.home</code></li>
     * <li>Environment variable <code>SLING_HOME</code></li>
     * <li>Default value <code>sling</code></li>
     * </ol>
     *
     * @param commandLine The command line arguments
     * @return The value to use for sling.home
     */
    private static String getSlingHome(Map<String, String> commandLine) {
        String source = null;

        String slingHome = commandLine.get(SharedConstants.SLING_HOME);
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

    /**
     * Return the absolute path to sling home
     */
    public String getSlingHome() {
        return this.slingHome;
    }

    /**
     * Define the sling.launchpad parameter implementing the algorithme defined
     * on the wiki page to find the setting according to this algorithm:
     * <ol>
     * <li>Configuration property <code>sling.launchpad</code>. This path is
     * resolved against the <code>slingHome</code> folder if relative.</li>
     * <li>Default to same as <code>sling.home</code></li>
     * </ol>
     *
     * @param slingHome The absolute path to the Sling Home folder (aka the
     *            <code>sling.home</code>.
     * @param commandLineArgs The configuration properties from where to get the
     *            <code>sling.launchpad</code> property.
     * @return The absolute <code>File</code> indicating the launchpad folder.
     */
    private static File getLaunchpadHome(final String slingHome,
            final Map<String, String> commandLineArgs) {
        final String launchpadHomeParam = commandLineArgs.get(SharedConstants.SLING_LAUNCHPAD);
        if (launchpadHomeParam == null || launchpadHomeParam.length() == 0) {
            commandLineArgs.put(SharedConstants.SLING_LAUNCHPAD, slingHome);
            return new File(slingHome);
        }

        File launchpadHome = new File(launchpadHomeParam);
        if (!launchpadHome.isAbsolute()) {
            launchpadHome = new File(slingHome, launchpadHomeParam);
        }

        commandLineArgs.put(SharedConstants.SLING_LAUNCHPAD,
            launchpadHome.getAbsolutePath());
        return launchpadHome;
    }

    // ---------- logging

    // emit an informational message to standard out
    protected static void info(String message, Throwable t) {
        log(System.out, "*INFO *", message, t);
    }

    // emit an error message to standard err
    protected static void error(String message, Throwable t) {
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
                public void print(String x) {
                    super.print(linePrefix);
                    super.print(x);
                }
            });
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
    // default accessor to enable unit tests wihtout requiring reflection
    static Map<String, String> parseCommandLine(String... args) {
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
                        final String val;
                        final int indexOfEq = arg.indexOf('=');
                        if (indexOfEq != -1) {
                            //Handle case -Da=b
                            key = arg.substring(1, indexOfEq);
                            val = arg.substring(indexOfEq + 1);
                        } else {
                            val = arg.substring(2);
                        }
                        commandLine.put(key, val);
                    } else {
                        argc++;
                        if (argc < args.length
                            && (args[argc].equals("-") || !args[argc].startsWith("-"))) {
                            String val = args[argc];

                            //Special handling for -D a=b
                            if(key.equals("D")){
                                final int indexOfEq = val.indexOf('=');
                                if (indexOfEq != -1) {
                                    //Handle case -D a=b. Add key as Da
                                    key = "D" + val.substring(0, indexOfEq);
                                    val = val.substring(indexOfEq + 1);
                                }
                            }
                            commandLine.put(key, val);
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

    /** prints a simple usage plus optional error message */
    private static boolean doHelp(Map<String, String> args) {
        if (args.remove("h") != null) {
            System.out.println("usage: "
                + Main.class.getName()
                + " [ start | stop | status ] [ -j adr ] [ -l loglevel ] [ -f logfile ] [ -c slinghome ] [ -i launchpadhome ] [ -a address ] [ -p port ] { -Dn=v } [ -h ]");

            System.out.println("    start         listen for control connection (uses -j)");
            System.out.println("    stop          terminate running Apache Sling (uses -j)");
            System.out.println("    status        check whether Apache Sling is running (uses -j)");
            System.out.println("    -j adr        host and port to use for control connection in the format '[host:]port' (default 127.0.0.1:0)");
            System.out.println("    -l loglevel   the initial loglevel (0..4, FATAL, ERROR, WARN, INFO, DEBUG)");
            System.out.println("    -f logfile    the log file, \"-\" for stdout (default logs/error.log)");
            System.out.println("    -c slinghome  the sling context directory (default sling)");
            System.out.println("    -i launchpadhome  the launchpad directory (default slinghome)");
            System.out.println("    -a address    the interfact to bind to (use 0.0.0.0 for any)");
            System.out.println("    -p port       the port to listen to (default 8080)");
            System.out.println("    -r path       the root servlet context path for the http service (default is /)");
            System.out.println("    -n            don't install the shutdown hook");
            System.out.println("    -Dn=v         sets property n to value v. Make sure to use this option *after* " +
                                                  "the jar filename. The JVM also has a -D option which has a " +
                                                  "different meaning");
            System.out.println("    -h            prints this usage message");

            return true;
        }
        return false;
    }

    private static boolean installShutdownHook(Map<String, String> props) {
        String prop = props.remove(PROP_SHUTDOWN_HOOK);
        if (prop == null) {
            prop = System.getProperty(PROP_SHUTDOWN_HOOK);
        }

        return (prop == null) ? true : Boolean.valueOf(prop);
    }

    // default accessor to enable unit tests wihtout requiring reflection
    static Map<String, String> convertCommandLineArgs(
            Map<String, String> rawArgs) {
        final HashMap<String, String> props = new HashMap<String, String>();
        boolean errorArg = false;
        for (Entry<String, String> arg : rawArgs.entrySet()) {
            if (arg.getKey().length() == 1 || arg.getKey().startsWith("D")) {
                String value = arg.getValue();
                switch (arg.getKey().charAt(0)) {
                    case 'j':
                        if (value == arg.getKey()) {
                            errorArg("-j", "Missing host:port value");
                            errorArg = true;
                            continue;
                        }
                        props.put(PROP_CONTROL_SOCKET, value);
                        break;

                    case 'l':
                        if (value == arg.getKey()) {
                            errorArg("-l", "Missing log level value");
                            errorArg = true;
                            continue;
                        }
                        props.put(PROP_LOG_LEVEL, value);
                        break;

                    case 'f':
                        if (value == arg.getKey()) {
                            errorArg("-f", "Missing log file value");
                            errorArg = true;
                            continue;
                        } else if ("-".equals(value)) {
                            value = "";
                        }
                        props.put(PROP_LOG_FILE, value);
                        break;

                    case 'c':
                        if (value == arg.getKey()) {
                            errorArg("-c", "Missing directory value");
                            errorArg = true;
                            continue;
                        }
                        props.put(SharedConstants.SLING_HOME, value);
                        break;

                    case 'i':
                        if (value == arg.getKey()) {
                            errorArg("-i", "Missing launchpad directory value");
                            errorArg = true;
                            continue;
                        }
                        props.put(SharedConstants.SLING_LAUNCHPAD, value);
                        break;

                    case 'a':
                        if (value == arg.getKey()) {
                            errorArg("-a", "Missing address value");
                            errorArg = true;
                            continue;
                        }
                        props.put(PROP_HOST, value);
                        break;

                    case 'p':
                        if (value == arg.getKey()) {
                            errorArg("-p", "Missing port value");
                            errorArg = true;
                            continue;
                        }
                        try {
                            // just to verify it is a number
                            Integer.parseInt(value);
                            props.put(PROP_PORT, value);
                        } catch (RuntimeException e) {
                            errorArg("-p", "Bad port: " + value);
                            errorArg = true;
                        }
                        break;

                    case 'r':
                        if (value == arg.getKey()) {
                            errorArg("-r", "Missing root path value");
                            errorArg = true;
                            continue;
                        }
                        props.put(PROP_CONTEXT_PATH, value);
                        break;

                    case 'n':
                        props.put(PROP_SHUTDOWN_HOOK, Boolean.FALSE.toString());
                        break;

                    case 'D':
                        if (value == arg.getKey()) {
                            errorArg("-D", "Missing property assignment");
                            errorArg = true;
                            continue;
                        }
                        if (arg.getKey().length() > 1) {
                            //Dfoo=bar arg.key=Dfoo and arg.value=bar
                            props.put(arg.getKey().substring(1), arg.getValue());
                        } else {
                            //D foo=bar arg.key=D and arg.value=foo=bar
                            String[] parts = value.split("=");
                            int valueIdx = (parts.length > 1) ? 1 : 0;
                            props.put(parts[0], parts[valueIdx]);
                        }
                        break;

                    default:
                        errorArg("-" + arg.getKey(), "Unrecognized option");
                        errorArg = true;
                        break;
                }
            } else if ("start".equals(arg.getKey())
                || "stop".equals(arg.getKey()) || "status".equals(arg.getKey())) {
                props.put(PROP_CONTROL_ACTION, arg.getValue());
            } else {
                errorArg(arg.getKey(), "Unrecognized option");
                errorArg = true;
            }
        }
        return errorArg ? null : props;
    }

    private static void errorArg(String option, String message) {
        error(String.format("%s: %s (use -h for more information)", option,
            message), null);
    }

    /**
     * Removes well-known stray threads and thread groups and removes framework
     * thread context class loaders. Well-known stray threads and thread groups
     * are:
     * <ul>
     * <li>The FileCleaningTracker$Reaper thread of the commons-io library</li>
     * <li>The QuartzScheduler:ApacheSling thread group. See <a
     * href="https://issues.apache.org/jira/browse/SLING-2535">SLING-2535
     * QuartzScheduler:ApacheSling thread group remaining after stopping the
     * scheduler bundle</a></li>
     * </ul>
     */
    static void cleanupThreads() {

        // the current thread is the SlingNotifier thread part of
        // the main thread group, whose parent is the system thread
        // group. We only care for the main thread group here
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        Thread[] active = new Thread[tg.activeCount()];
        tg.enumerate(active);
        for (Thread thread : active) {
            if (thread != null) {
                if (thread.getName().equals("FileCleaningTracker$Reaper")) {
                    // I know, but this thread is stray ...
                    // Commons-IO bundle (or consumer of it) should
                    // actually stop it
                    thread.stop();
                } else {
                    ClassLoader loader = thread.getContextClassLoader();
                    if (loader != null && loader.getClass().getName().startsWith("org.apache.felix.framework.")) {
                        thread.setContextClassLoader(null);
                    }
                }
            }
        }

        // SLING-2535 - Scheduler thread group
        ThreadGroup[] groups = new ThreadGroup[tg.activeGroupCount()];
        tg.enumerate(groups);
        for (ThreadGroup group : groups) {
            if (group != null && group.getName().equals("QuartzScheduler:ApacheSling")) {
                group.destroy();
            }
        }
    }

    private class ShutdownHook implements Runnable {
        public void run() {
            Main.info("Java VM is shutting down", null);
            Main.this.doStop();
        }
    }

    private class Notified implements Notifiable {

        /**
         * The framework has been stopped by calling the
         * <code>Bundle.stop()</code> on the system bundle. This actually
         * terminates the Sling Standalone application.
         */
        public void stopped() {
            /**
             * This method is called if the framework is stopped from within by
             * calling stop on the system bundle or if the framework is stopped
             * because the VM is going down and the shutdown hook has initated
             * the shutdown In any case we ensure the reference to the framework
             * is removed and remove the shutdown hook (but don't care if that
             * fails).
             */

            Main.info("Apache Sling has been stopped", null);

            Main.this.sling = null;
            Main.this.doStop();
        }

        /**
         * The framework has been stopped with the intent to be restarted by
         * calling either of the <code>Bundle.update</code> methods on the
         * system bundle.
         * <p>
         * If an <code>InputStream</code> was provided, this has been copied to
         * a temporary file, which will be used in place of the existing
         * launcher jar file.
         *
         * @param updateFile The temporary file to replace the existing launcher
         *            jar file. If <code>null</code> the existing launcher jar
         *            will be used again.
         */
        public void updated(File updateFile) {

            Main.this.sling = null;
            Main.this.doStop();

            Main.cleanupThreads();

            if (updateFile == null) {

                Main.info("Restarting Framework and Apache Sling", null);
                if (!Main.this.doStart(null)) {
                    Main.error("Failed to restart Sling; terminating", null);
                    Main.this.terminateVM(1);
                }

            } else {

                Main.info(
                    "Restarting Framework with update from " + updateFile, null);
                boolean started = false;
                try {
                    started = Main.this.doStart(updateFile.toURI().toURL());
                } catch (MalformedURLException mue) {
                    Main.error("Cannot get URL for file " + updateFile, mue);
                } finally {
                    updateFile.delete();
                }

                if (!started) {
                    Main.error("Failed to restart Sling; terminating", null);
                    Main.this.terminateVM(1);
                }
            }
        }
    }

}
