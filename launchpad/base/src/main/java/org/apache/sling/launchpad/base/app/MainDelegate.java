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
package org.apache.sling.launchpad.base.app;

import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.base.impl.ClassLoaderResourceProvider;
import org.apache.sling.launchpad.base.impl.Sling;
import org.apache.sling.launchpad.base.shared.Launcher;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.apache.sling.launchpad.base.shared.SharedConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

/**
 * The <code>Main</code> class is a simple Java Application which interprests
 * the command line and creates the {@link Sling} launcher class and thus starts
 * the OSGi framework. In addition a shutdown thread is registered to ensure
 * proper shutdown on VM termination.
 * <p>
 * The supported command line options are:
 * <dl>
 * <dt>-l loglevel</dt>
 * <dd>Sets the initial loglevel as an integer in the range 0 to 4 or as one of
 * the well known level strings FATAL, ERROR, WARN, INFO or DEBUG. This option
 * overwrites the <code>org.apache.sling.osg.log.level</code> setting the
 * <code>sling.properties</code> file.</dd>
 * <dt>-f logfile</dt>
 * <dd>The log file, \"-\" for stdout (default logs/error.log). This option
 * overwrites the <code>org.apache.sling.osg.log.file</code> setting the
 * <code>sling.properties</code> file.</dd>
 * <dt>-c slinghome</dt>
 * <dd>The directory in which Sling locates its initial configuration file
 * <code>sling.properties</code> and where files of Sling itself such as the
 * Apache Felix bundle archive or the JCR repository files are stored (default
 * sling).</dd>
 * <dt>-a address</dt>
 * <dd>The interfact to bind to (use 0.0.0.0 for any). This option is not
 * implemented yet.</dd>
 * <dt>-p port</dt>
 * <dd>The port to listen (default 8080) to handle HTTP requests. This option
 * overwrites the <code>org.osgi.service.http.port</code> setting the
 * <code>sling.properties</code> file.</dd>
 * <dt>-h</dt>
 * <dd>Prints a simple usage message listing all available command line options.
 * </dd>
 * </dl>
 */
public class MainDelegate implements Launcher {

    /** Mapping between log level numbers and names */
    private static final String[] logLevels = { "FATAL", "ERROR", "WARN",
        "INFO", "DEBUG" };

    /** The Sling configuration property name setting the initial log level */
    private static final String PROP_LOG_LEVEL = "org.apache.sling.commons.log.level";

    /** The Sling configuration property name setting the initial log file */
    private static final String PROP_LOG_FILE = "org.apache.sling.commons.log.file";

    /** The Sling system property name setting the bootstrap log level */
    private static final String PROP_BOOT_LOG_LEVEL = "sling.launchpad.log.level";

    /** Default log level setting if no set on command line (value is "INFO"). */
    private static final int DEFAULT_LOG_LEVEL = Logger.LOG_INFO;

    /**
     * The configuration property setting the port on which the HTTP service
     * listens
     */
    private static final String PROP_PORT = "org.osgi.service.http.port";

    /** The default port on which the HTTP service listens. */
    private static final String DEFAULT_PORT = "8080";

    private Notifiable notifiable;

    /** The parsed command line mapping (Sling) option name to option value */
    private Map<String, String> commandLine;

    private String slingHome;

    private Sling sling;

    public void setNotifiable(Notifiable notifiable) {
        this.notifiable = notifiable;
    }

    public void setCommandLine(Map<String, String> args) {
        commandLine = new HashMap<String, String>();
        commandLine.put(PROP_PORT, DEFAULT_PORT);
        parseCommandLine(args, commandLine);
    }

    public void setSlingHome(String slingHome) {
        this.slingHome = slingHome;
    }

    public boolean start() {

        Map<String, String> props = new HashMap<String, String>();

        // parse the command line (exit in case of failure)
        if (commandLine == null) {
            setCommandLine(new HashMap<String, String>());
        }

        // if sling.home was set on the command line, set it in the properties
        if (slingHome != null) {
            props.put(SharedConstants.SLING_HOME, slingHome);
        } else if (commandLine.containsKey(SharedConstants.SLING_HOME)) {
            props.put(SharedConstants.SLING_HOME,
                commandLine.get(SharedConstants.SLING_HOME));
        }

        // ensure sling.launchpad is set
        if (!commandLine.containsKey(SharedConstants.SLING_LAUNCHPAD)) {
            commandLine.put(SharedConstants.SLING_LAUNCHPAD, slingHome);
        }

        // check sling.properties in the command line
        final String slingPropertiesProp = commandLine.remove(SharedConstants.SLING_PROPERTIES);
        if (slingPropertiesProp != null) {
            props.put(SharedConstants.SLING_PROPERTIES, slingPropertiesProp);
        }

        // set up and configure Felix Logger
        int logLevel;
        if (!commandLine.containsKey(PROP_LOG_LEVEL)) {
            logLevel = DEFAULT_LOG_LEVEL;
        } else {
            logLevel = toLogLevelInt(commandLine.get(PROP_LOG_LEVEL),
                DEFAULT_LOG_LEVEL);
            commandLine.put(LOG_LEVEL_PROP, String.valueOf(logLevel));
        }
        final Logger logger = new SlingLogger();

        // Display port number on console, in case HttpService doesn't
        info("HTTP server port: " + commandLine.get(PROP_PORT), null);

        // default log level: prevent tons of needless WARN from the framework
        logger.setLogLevel(Logger.LOG_ERROR);
        if ( System.getProperty(PROP_BOOT_LOG_LEVEL) != null ) {
            try {
                logger.setLogLevel(
                    Integer.parseInt(System.getProperty(PROP_BOOT_LOG_LEVEL)));
            } catch (final NumberFormatException ex) {
                // just ignore
            }
        }

        try {
            LaunchpadContentProvider resProvider = new ClassLoaderResourceProvider(
                getClass().getClassLoader());

            // creating the instance launches the framework and we are done here
            // ..
            sling = new Sling(notifiable, logger, resProvider, props) {

                // overwrite the loadPropertiesOverride method to inject the
                // command line arguments unconditionally. These will not be
                // persisted in any properties file, though
                protected void loadPropertiesOverride(
                        Map<String, String> properties) {
                    if (commandLine != null) {
                        properties.putAll(commandLine);
                    }
                }
            };

            // we successfully started it
            return true;

        } catch (BundleException be) {
            error("Failed to Start OSGi framework", be);
        }

        // we failed to start
        return false;
    }

    public void stop() {
        if (sling != null) {
            sling.destroy();
            sling = null;
        }
    }

    /**
     * Parses the command line in <code>args</code> and sets appropriate Sling
     * configuration options in the <code>props</code> map.
     */
    private static void parseCommandLine(Map<String, String> args,
            Map<String, String> props) {

        /*
         * NOTE: We expect command line args to be suitable to use as
         * properties to launch Sling. Any standalone Java application
         * command line args have to be translated into Sling launcher
         * properties by the Main class. For deployment of the Launchpad
         * JAR into older launchers we keep converting existing command
         * line args for now. New command line arguments must solely be
         * known and converted in the Main class and not here.
         */

        for (Entry<String, String> arg : args.entrySet()) {
            if (arg.getKey().length() == 1) {
                String value = arg.getValue();
                switch (arg.getKey().charAt(0)) {
                    case 'l':
                        if (value == arg.getKey()) {
                            terminate("Missing log level value", 1);
                            continue;
                        }
                        props.put(PROP_LOG_LEVEL, value);
                        break;

                    case 'f':
                        if (value == arg.getKey()) {
                            terminate("Missing log file value", 1);
                            continue;
                        } else if ("-".equals(value)) {
                            value = "";
                        }
                        props.put(PROP_LOG_FILE, value);
                        break;

                    case 'c':
                        if (value == arg.getKey()) {
                            terminate("Missing directory value", 1);
                            continue;
                        }
                        props.put(SharedConstants.SLING_HOME, value);
                        break;

                    case 'p':
                        if (value == arg.getKey()) {
                            terminate("Missing port value", 1);
                            continue;
                        }
                        try {
                            // just to verify it is a number
                            Integer.parseInt(value);
                            props.put(PROP_PORT, value);
                        } catch (RuntimeException e) {
                            terminate("Bad port: " + value, 1);
                        }
                        break;

                    case 'a':
                        if (value == arg.getKey()) {
                            terminate("Missing address value", 1);
                            continue;
                        }
                        info("Setting the address to bind to is not supported, binding to 0.0.0.0", null);
                        break;

                    default:
                        terminate("Unrecognized option " + arg.getKey(), 1);
                        break;
                }
            } else {
                info("Setting " + arg.getKey() + "=" + arg.getValue(), null);
                props.put(arg.getKey(), arg.getValue());
            }
        }
    }

    /** Return the log level code for the string */
    private static int toLogLevelInt(String level, int defaultLevel) {
        try {
            int logLevel = Integer.parseInt(level);
            if (logLevel >= 0 && logLevel < logLevels.length) {
                return logLevel;
            }
        } catch (NumberFormatException nfe) {
            // might be a log level string
        }

        for (int i = 0; i < logLevels.length; i++) {
            if (logLevels[i].equalsIgnoreCase(level)) {
                return i;
            }
        }

        return defaultLevel;
    }

    // ---------- console logging

    /** prints a simple usage plus optional error message and exists with code */
    private static void terminate(String message, int code) {
        if (message != null) {
            error(message + " (use -h for more information)", null);
        }

        System.exit(code);
    }

    // emit an debugging message to standard out
    static void debug(String message, Throwable t) {
        log(System.out, "*DEBUG*", message, t);
    }

    // emit an informational message to standard out
    static void info(String message, Throwable t) {
        log(System.out, "*INFO *", message, t);
    }

    // emit an warning message to standard out
    static void warn(String message, Throwable t) {
        log(System.out, "*WARN *", message, t);
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

        synchronized (out) {
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
    }

    private static class SlingLogger extends Logger {

        @Override
        protected void doLog(Bundle bundle, ServiceReference sr, int level, String msg, Throwable throwable) {

            // unwind throwable if it is a BundleException
            if ((throwable instanceof BundleException) && (((BundleException) throwable).getNestedException() != null)) {
                throwable = ((BundleException) throwable).getNestedException();
            }

            String s = (sr == null) ? null : "SvcRef " + sr;
            s = (s == null) ? null : s + " Bundle '" + bundle.getBundleId() + "'";
            s = (s == null) ? msg : s + " " + msg;
            s = (throwable == null) ? s : s + " (" + throwable + ")";

            switch (level) {
                case LOG_DEBUG:
                    debug("DEBUG: " + s, null);
                    break;
                case LOG_INFO:
                    info("INFO: " + s, null);
                    break;
                case LOG_WARNING:
                    warn("WARNING: " + s, null);
                    break;
                case LOG_ERROR:
                    error("ERROR: " + s, throwable);
                    break;
                default:
                    warn("UNKNOWN[" + level + "]: " + s, null);
            }
        }
    }
}
