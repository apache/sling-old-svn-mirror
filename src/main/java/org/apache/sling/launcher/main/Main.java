/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launcher.main;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.sling.launcher.Sling;
import org.osgi.framework.BundleException;

/**
 * The <code>Sling</code> serves as a basic servlet for Project Sling. The
 * tasks of this servlet are as follows:
 * <ul>
 * <li>The {@link #init()} method launches Apache <code>Felix</code> as the
 * OSGi framework implementation we use.
 * <li>Registers as a service listener interested for services of type
 * <code>javax.servlet.Servlet</code>.
 * <li>Handles requests by delegating to a servlet which is expected to be
 * registered with the framework as a service of type
 * <code>javax.servlet.Servlet</code>. If no delegatee servlet has been
 * registered request handlings results in a temporary unavailability of the
 * servlet.
 * </ul>
 * <p>
 * <b>Request Handling</b>
 * <p>
 * This servlet handles request by forwarding to a delegatee servlet. The
 * delegatee servlet is automatically retrieved from the service registry by the
 * {@link #getDelegatee()}. This method also makes sure, the such a servlet
 * actually exits by throwing an <code>UnvailableException</code> if not and
 * also makes sure the servlet is initialized.
 * <p>
 * <b>Launch Configuration</b>
 * <p>
 * The Apache <code>Felix</code> framework requires configuration parameters
 * to be specified for startup. This servlet builds the list of parameters from
 * three locations:
 * <ol>
 * <li>The <code>com/day/osgi/servlet/Sling.properties</code> is read from
 * the servlet class path. This properties file contains default settings.</li>
 * <li>Extensions of this servlet may provide additional properties to be
 * loaded overwriting the {@link #loadPropertiesOverride(Properties)} method.
 * <li>Finally, web application init parameters are added to the properties and
 * may overwrite existing properties of the same name(s).
 * </ol>
 * <p>
 * After loading all properties, variable substitution takes place on the
 * property values. A variable is indicated as <code>${&lt;prop-name&gt;}</code>
 * where <code>&lt;prop-name&gt;</code> is the name of a system or
 * configuration property (configuration properties override system properties).
 * Variables may be nested and are resolved from inner-most to outer-most. For
 * example, the property value <code>${outer-${inner}}</code> is resolved by
 * first resolving <code>${inner}</code> and then resolving the property whose
 * name is the catenation of <code>outer-</code> and the result of resolving
 * <code>${inner}</code>.
 * <p>
 * <b>Logging</b>
 * <p>
 * This servlet logs through the servlet container logging mechanism by calling
 * the <code>GenericServlet.log</code> methods. Bundles launched within the
 * framework provided by this servlet may use whatever logging mechanism they
 * choose to use. The Day Commons OSGI Log Bundle provides an OSGi Log Service
 * implementation, which also provides access to Apache Commons Logging, SLF4J
 * and Log4J logging. It is recommended that this bundle is used to setup and
 * configure logging for systems based on this servlet.
 */
public class Main {

    /** Mapping between log level numbers and names */
    private static final String[] logLevels = { "FATAL", "ERROR", "WARN",
        "INFO", "DEBUG" };

    private static final String PROP_LOG_LEVEL = "org.apache.sling.log.level";

    private static final String PROP_LOG_FILE = "org.apache.sling.log.file";

    private static final String PROP_PORT = "org.osgi.service.http.port";

    private static final String DEFAULT_PORT = "8080";

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

        try {
            Sling sling = new Sling(null, null, props) {
                protected void loadPropertiesOverride(
                        Map<String, String> properties) {
                    if (commandLine != null) {
                        properties.putAll(commandLine);
                    }
                }
            };

            Runtime.getRuntime().addShutdownHook(new TerminateSling(sling));
        } catch (BundleException be) {
            System.err.println("Failed to Start OSGi framework");
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

    private static void parseCommandLine(String[] args,
            Map<String, String> props) {
        int argc = 0;
        while (argc < args.length) {
            String arg = args[argc++];
            if (arg.startsWith("-")) {
                if (arg.length() != 2) {
                    usage("Missing option name", 1);
                }
                String value = argc < args.length ? args[argc++] : null;
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
                    case 'd':
                        log("The -d command line parameter is deprecated: use -c instead.");
                        // fall through
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
                        // try {
                        // // just to verify it is a number
                        // InetAddress.getByName(value);
                        // } catch (UnknownHostException e) {
                        // log("Bad address: " + value);
                        // }
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

    private static void usage(String message, int code) {
        if (message != null) {
            log(message);
            System.err.println();
        }

        System.err.println("usage: "
            + Main.class.getName()
            + " [ -l loglevel ] [ -f logfile ] [ -c slinghome ] [ -a address ] [ -p port ] [ -h ]");

        System.err.println("    -l loglevel   the initial loglevel (0..4, FATAL, ERROR, WARN, INFO, DEBUG)");
        System.err.println("    -f logfile    the log file, \"-\" for stdout (default logs/error.log)");
        System.err.println("    -c slinghome  the sling context directory (default sling)");
        System.err.println("    -a address    the interfact to bind to (use 0.0.0.0 for any) (not supported yet)");
        System.err.println("    -p port       the port to listen to (default 8080)");
        System.err.println("    -h            prints this usage message");

        // exiting now
        System.exit(code);
    }

    private static void log(String message) {
        System.err.println(message);
    }

    private static String toLogLevel(int level) {
        if (level >= 0 && level < logLevels.length) {
            return logLevels[level];
        }

        usage("Bad log level: " + level, 1);
        return null;
    }

    private static String checkLogLevel(String level) {
        for (int i = 0; i < logLevels.length; i++) {
            if (logLevels[i].equalsIgnoreCase(level)) {
                return logLevels[i];
            }
        }

        usage("Bad log level: " + level, 1);
        return null;
    }
}
