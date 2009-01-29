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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    private final String[] commandLineArgs;

    private final String slingHome;

    private Launcher sling;

    private Main(String[] args) {

        // set the thread name
        super("Sling Terminator");

        // sling.home from the command line or system properties, else default
        String slingHome = getSlingHome(args);
        info("Starting Sling in " + slingHome, null);

        this.commandLineArgs = args;
        this.slingHome = slingHome;

        Runtime.getRuntime().addShutdownHook(this);

        // ensure up-to-date launcher jar
        startSling(getClass().getResource(
            SharedConstants.DEFAULT_SLING_LAUNCHER_JAR));
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

        info("Sling has been stopped", null);

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
        if (updateFile == null) {

            info("Restarting Framework and Sling", null);
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
        if (sling != null) {
            info("Stopping Sling", null);
            sling.stop();
        }
    }

    // ---------- internal

    private void startSling(URL launcherJar) {
        if (launcherJar != null) {
            try {
                info("Checking launcher JAR in " + slingHome, null);
                if (Loader.installLauncherJar(launcherJar, slingHome)) {
                    info("Installed or Updated launcher JAR file from "
                        + launcherJar, null);
                } else {
                    info("Existing launcher JAR file already up to date", null);
                }
            } catch (IOException ioe) {
                error("Failed installing " + launcherJar, ioe);
            }
        } else {
            info("No Launcher JAR to install", null);
        }

        Object object;
        try {
            info(
                "Loading launcher class " + SharedConstants.DEFAULT_SLING_MAIN,
                null);
            object = Loader.loadLauncher(SharedConstants.DEFAULT_SLING_MAIN,
                slingHome);
        } catch (IllegalArgumentException iae) {
            error("Failed loading Sling class "
                + SharedConstants.DEFAULT_SLING_MAIN, iae);
            return;
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
                error("There was a problem launching Sling", null);
            }
        }
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
    private static String getSlingHome(String[] args) {
        String source = null;
        String slingHome = null;

        for (int argc = 0; argc < args.length; argc++) {
            String arg = args[argc];
            if (arg.startsWith("-") && arg.length() == 2
                && arg.charAt(1) == 'c') {
                argc++;
                if (argc < args.length) {
                    source = "command line";
                    slingHome = args[argc];
                }
                break;
            }
        }

        if (slingHome == null) {
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

    // ---------- logging

    // emit an informational message to standard out
    private static void info(String message, Throwable t) {
        log(System.out, "*INFO*", message, t);
    }

    // emit an error message to standard err
    private static void error(String message, Throwable t) {
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
}
