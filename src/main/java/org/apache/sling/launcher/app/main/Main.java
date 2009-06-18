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
package org.apache.sling.launcher.app.main;

import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.framework.Logger;
import org.apache.sling.commons.log.LogManager;
import org.apache.sling.launcher.app.ClassLoaderResourceProvider;
import org.apache.sling.launcher.app.ResourceProvider;
import org.apache.sling.launcher.app.Sling;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

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
 * <dd>Prints a simple usage message listing all available command line
 * options.</dd>
 * </dl>
 */
public class Main {

    /** Mapping between log level numbers and names */
    private static final String[] logLevels = { "FATAL", "ERROR", "WARN",
        "INFO", "DEBUG" };

    /** The Sling configuration property name setting the initial log level */
    private static final String PROP_LOG_LEVEL = LogManager.LOG_LEVEL;

    /** The Sling configuration property name setting the initial log file */
    private static final String PROP_LOG_FILE = LogManager.LOG_FILE;

    /** Default log level setting if no set on command line (value is "INFO"). */
    private static final int DEFAULT_LOG_LEVEL = Logger.LOG_INFO;

    /**
     * The configuration property setting the port on which the HTTP service
     * listens
     */
    private static final String PROP_PORT = "org.osgi.service.http.port";

    /** The default port on which the HTTP service listens. */
    private static final String DEFAULT_PORT = "8080";

    /**
     * The property value to export the Servlet API 2.5 from the system
     * bundle.
     */
    private static final String SERVLET_API_EXPORT =
        "javax.servlet;javax.servlet.http;javax.servlet.resources; version=2.5";

    /** The parsed command line mapping (Sling) option name to option value */
    private static Map<String, String> commandLine;

    public static void main(String[] args) throws Exception {
        // creating the instance launches the framework and we are done here ..
        Map<String, String> props = new HashMap<String, String>();

        // parse the command line (exit in case of failure)
        commandLine = new HashMap<String, String>();
        commandLine.put(PROP_PORT, DEFAULT_PORT);
        parseCommandLine(args, commandLine);

        // if sling.home was set on the command line, set it in the properties
        if (commandLine.containsKey(Sling.SLING_HOME)) {
            props.put(Sling.SLING_HOME, commandLine.get(Sling.SLING_HOME));
        }

        // overwrite the loadPropertiesOverride method to inject the command
        // line arguments unconditionally. These will not be persisted in any
        // properties file, though

        // set up and configure Felix Logger
        int logLevel;
        if (!commandLine.containsKey(PROP_LOG_LEVEL)) {
            logLevel = DEFAULT_LOG_LEVEL;
        } else {
            logLevel = toLogLevelInt(commandLine.get(PROP_LOG_LEVEL),
                DEFAULT_LOG_LEVEL);
            commandLine.put(LOG_LEVEL_PROP, String.valueOf(logLevel));
        }
        Logger logger = new Logger();

        // prevent tons of needless WARN from the framework
        // logger.setLogLevel(logLevel);
        logger.setLogLevel(Logger.LOG_ERROR);

        // prevent tons of needless WARN messages from the framework
        // logger.setLogLevel(logLevel);
        logger.setLogLevel(Logger.LOG_ERROR);

        try {
            ResourceProvider resProvider = new ClassLoaderResourceProvider(
                Main.class.getClassLoader());
            Sling sling = new Sling(logger, resProvider, props) {
                protected void loadPropertiesOverride(
                        Map<String, String> properties) {
                    if (commandLine != null) {
                        properties.putAll(commandLine);
                    }

                    // add Servlet API to the system bundle exports
                    String sysExport = properties.get(Constants.FRAMEWORK_SYSTEMPACKAGES);
                    if (sysExport == null) {
                        sysExport = SERVLET_API_EXPORT;
                    } else {
                        sysExport += "," + SERVLET_API_EXPORT;
                    }
                    properties.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
                        sysExport);
                }
            };

            Runtime.getRuntime().addShutdownHook(new TerminateSling(sling));
        } catch (BundleException be) {
            log("Failed to Start OSGi framework");
            be.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private static class TerminateSling extends Thread {
        private final Sling sling;

        TerminateSling(Sling sling) {
            super("Sling Terminator");
            this.sling = sling;
        }

        public void run() {
            if (this.sling != null) {
                this.sling.destroy();
            }
        }
    }

    /**
     * Parses the command line in <code>args</code> and sets appropriate Sling
     * configuration options in the <code>props</code> map.
     */
    private static void parseCommandLine(String[] args,
            Map<String, String> props) {
        for (int argc = 0; argc < args.length; argc++) {
            String arg = args[argc];
            if (arg.startsWith("-")) {

                // require at least another character naming the option
                if (arg.length() != 2) {
                    usage("Missing option name", 1);
                }

                // option argument is following the current option
                argc++;
                String value = argc < args.length ? args[argc] : null;

                switch (arg.charAt(1)) {
                    case 'l':
                        if (value == null) {
                            usage("Missing log level value", 1);
                            continue;
                        }
                        try {
                            int logLevel = Integer.parseInt(value);
                            value = toLogLevel(logLevel);
                        } catch (NumberFormatException nfe) {
                            // might be a log level string
                            value = checkLogLevel(value);
                        }
                        if (value != null) {
                            props.put(PROP_LOG_LEVEL, value);
                        }
                        break;

                    case 'f':
                        if (value == null) {
                            usage("Missing log file value", 1);
                            continue;
                        } else if ("-".equals(value)) {
                            value = "";
                        }
                        props.put(PROP_LOG_FILE, value);
                        break;

                    case 'c':
                        if (value == null) {
                            usage("Missing directory value", 1);
                            continue;
                        }
                        props.put(Sling.SLING_HOME, value);
                        break;

                    case 'p':
                        if (value == null) {
                            usage("Missing port value", 1);
                            continue;
                        }
                        try {
                            // just to verify it is a number
                            Integer.parseInt(value);
                            props.put(PROP_PORT, value);
                        } catch (RuntimeException e) {
                            usage("Bad port: " + value, 1);
                        }
                        break;

                    case 'a':
                        if (value == null) {
                            usage("Missing address value", 1);
                            continue;
                        }
                        log("Setting the address to bind to is not supported, binding to 0.0.0.0");
                        break;

                    case 'h':
                        usage(null, 0);

                    default:
                        usage("Unrecognized option " + arg, 1);
                        break;
                }
            }
        }
    }

    /** prints a simple usage plus optional error message and exists with code */
    private static void usage(String message, int code) {
        if (message != null) {
            log(message);
            log("");
        }

        log("usage: "
            + Main.class.getName()
            + " [ -l loglevel ] [ -f logfile ] [ -c slinghome ] [ -a address ] [ -p port ] [ -h ]");

        log("    -l loglevel   the initial loglevel (0..4, FATAL, ERROR, WARN, INFO, DEBUG)");
        log("    -f logfile    the log file, \"-\" for stdout (default logs/error.log)");
        log("    -c slinghome  the sling context directory (default sling)");
        log("    -a address    the interfact to bind to (use 0.0.0.0 for any) (not supported yet)");
        log("    -p port       the port to listen to (default 8080)");
        log("    -h            prints this usage message");

        // exiting now
        System.exit(code);
    }

    /** Writes the message to stderr output */
    private static void log(String message) {
        System.err.println(message);
    }

    /** Converts the loglevel code to a loglevel string name */
    private static String toLogLevel(int level) {
        if (level >= 0 && level < logLevels.length) {
            return logLevels[level];
        }

        usage("Bad log level: " + level, 1);
        return null;
    }

    /** Verifies the log level is one of the known values, returns null otherwise */
    private static String checkLogLevel(String level) {
        for (int i = 0; i < logLevels.length; i++) {
            if (logLevels[i].equalsIgnoreCase(level)) {
                return logLevels[i];
            }
        }

        usage("Bad log level: " + level, 1);
        return null;
    }

    /** Return the log level code for the string */
    private static int toLogLevelInt(String level, int defaultLevel) {
        for (int i = 0; i < logLevels.length; i++) {
            if (logLevels[i].equalsIgnoreCase(level)) {
                return i;
            }
        }

        return defaultLevel;
    }
}
