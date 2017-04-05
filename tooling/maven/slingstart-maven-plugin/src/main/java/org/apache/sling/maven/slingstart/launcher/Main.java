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
package org.apache.sling.maven.slingstart.launcher;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Main class for launching Apache Sling.
 *
 */
public class Main {

    /** Arguments to pass to the real main class */
    private final String[] startupArgs;

    /** Verbose flag */
    private final boolean verbose;

    /** App jar */
    private final File appJar;

    /** Listener port. */
    private final int listenerPort;

    /** Main class default value */
    private final static String MAIN_CLASS_DEF = "org.apache.sling.launchpad.app.Main";

    /** Delimeter string */
    private final static String DELIM =
	    "-------------------------------------------------------------------";

    /**
     * Create a new launcher
     * First argument is the launchpad jar
     * Second argument is the listener port
     * Third argument is verbose
     */
    public Main(final String[] args) {
        if ( args == null || args.length < 3 ) {
            throw new IllegalArgumentException("Missing configuration: " + args);
        }
        this.appJar = new File(args[0]);
        this.listenerPort = Integer.valueOf(args[1]);
        this.verbose = Boolean.valueOf(args[2]);
	    this.startupArgs = new String[args.length-3];
	    System.arraycopy(args, 3, this.startupArgs, 0, this.startupArgs.length);
    }

    /**
     * Startup
     */
    public void run() throws Exception {
        if (verbose) {
	        System.out.println(DELIM);
            System.out.println("Slingstart application: " + this.appJar);
            System.out.println("Main class: " + MAIN_CLASS_DEF);
            System.out.println("Listener Port: " + String.valueOf(this.listenerPort));
            System.out.println("Arguments: " + Arrays.toString(this.startupArgs));
            System.out.println(DELIM);
        }

        final ClassLoader cl = new URLClassLoader(new URL[] {this.appJar.toURI().toURL()});
        Thread.currentThread().setContextClassLoader(cl);

        // create and register mbean
        final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
        jmxServer.registerMBean(new Launcher(this.listenerPort),
                new ObjectName("org.apache.sling.launchpad:type=Launcher"));

        final Class<?> mainClass = cl.loadClass(MAIN_CLASS_DEF);
        final Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
        mainMethod.invoke(null, (Object)this.startupArgs);
    }

    public static void main(final String[] args) {
        try {
            final Main m = new Main(args);
            m.run();
        } catch ( final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

