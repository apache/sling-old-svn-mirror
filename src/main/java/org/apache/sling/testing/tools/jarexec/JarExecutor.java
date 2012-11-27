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
package org.apache.sling.testing.tools.jarexec;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Start a runnable jar by forking a JVM process,
 *  and terminate the process when this VM exits.
 */
public class JarExecutor {
    private final File jarToExecute;
    private final String jvmFullPath;
    private final int serverPort;
    private final Properties config;
    private Executor executor;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_PORT = 8765;
    public static final int DEFAULT_EXIT_TIMEOUT = 30;

    public static final String DEFAULT_JAR_FOLDER = "target/dependency";
    public static final String DEFAULT_JAR_NAME_REGEXP = "org.apache.sling.*jar$";
    public static final String PROP_PREFIX = "jar.executor.";
    public static final String PROP_SERVER_PORT = PROP_PREFIX + "server.port";
    public static final String PROP_JAR_FOLDER = PROP_PREFIX + "jar.folder";
    public static final String PROP_JAR_NAME_REGEXP = PROP_PREFIX + "jar.name.regexp";
    public static final String PROP_VM_OPTIONS = PROP_PREFIX + "vm.options";
    public static final String PROP_WORK_FOLDER = PROP_PREFIX + "work.folder";
    public static final String PROP_JAR_OPTIONS = PROP_PREFIX + "jar.options";
    public static final String PROP_EXIT_TIMEOUT_SECONDS = PROP_PREFIX + "exit.timeout.seconds";

    @SuppressWarnings("serial")
    public static class ExecutorException extends Exception {
        ExecutorException(String reason) {
            super(reason);
        }
        ExecutorException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + jarToExecute.getName() + " (port " + serverPort + ")";
    }

    public int getServerPort() {
        return serverPort;
    }

    /** Build a JarExecutor, locate the jar to run, etc */
    public JarExecutor(Properties config) throws ExecutorException {
        this.config = config;
        final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

        String portStr = config.getProperty(PROP_SERVER_PORT);
        serverPort = portStr == null ? DEFAULT_PORT : Integer.valueOf(portStr);

        final String javaExecutable = isWindows ? "java.exe" : "java";
        jvmFullPath = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + javaExecutable;

        String jarFolderPath = config.getProperty(PROP_JAR_FOLDER);
        jarFolderPath = jarFolderPath == null ? DEFAULT_JAR_FOLDER : jarFolderPath;
        final File jarFolder = new File(jarFolderPath);

        String jarNameRegexp = config.getProperty(PROP_JAR_NAME_REGEXP);
        jarNameRegexp = jarNameRegexp == null ? DEFAULT_JAR_NAME_REGEXP : jarNameRegexp;
        final Pattern jarPattern = Pattern.compile(jarNameRegexp);

        // Find executable jar
        final String [] candidates = jarFolder.list();
        if(candidates == null) {
            throw new ExecutorException(
                    "No files found in jar folder specified by "
                    + PROP_JAR_FOLDER + " property: " + jarFolder.getAbsolutePath());
        }
        File f = null;
        for(String filename : candidates) {
            if(jarPattern.matcher(filename).matches()) {
                f = new File(jarFolder, filename);
                break;
            }
        }

        if(f == null) {
            throw new ExecutorException("Executable jar matching '" + jarPattern
                    + "' not found in " + jarFolder.getAbsolutePath()
                    + ", candidates are " + Arrays.asList(candidates));
        }
        jarToExecute = f;
    }

    /** Start the jar if not done yet, and setup runtime hook
     *  to stop it.
     */
    public void start() throws Exception {
        final ExecuteResultHandler h = new ExecuteResultHandler() {
            public void onProcessFailed(ExecuteException ex) {
                log.error("Process execution failed:" + ex, ex);
            }

            public void onProcessComplete(int result) {
                log.info("Process execution complete, exit code=" + result);
            }
        };

        final String vmOptions = config.getProperty(PROP_VM_OPTIONS);
        executor = new DefaultExecutor();
        final CommandLine cl = new CommandLine(jvmFullPath);
        if (vmOptions != null && vmOptions.length() > 0) {
            cl.addArguments(vmOptions);
        }
        cl.addArgument("-jar");
        cl.addArgument(jarToExecute.getAbsolutePath());

        // Additional options for the jar that's executed.
        // $JAREXEC_SERVER_PORT$ is replaced our serverPort value
        String jarOptions = config.getProperty(PROP_JAR_OPTIONS);
        if(jarOptions != null && jarOptions.length() > 0) {
            jarOptions = jarOptions.replaceAll("\\$JAREXEC_SERVER_PORT\\$", String.valueOf(serverPort));
            log.info("Executable jar options: {}", jarOptions);
            cl.addArguments(jarOptions);
        }

        final String workFolderOption = config.getProperty(PROP_WORK_FOLDER);
        if(workFolderOption != null && workFolderOption.length() > 0) {
            final File workFolder = new File(workFolderOption);
            if(!workFolder.isDirectory()) {
                throw new IOException("Work dir set by " + PROP_WORK_FOLDER + " option does not exist: "
                        + workFolder.getAbsolutePath());
            }
            log.info("Setting working directory for executable jar: {}", workFolder.getAbsolutePath());
            executor.setWorkingDirectory(workFolder);
        }

        String tmStr = config.getProperty(PROP_EXIT_TIMEOUT_SECONDS);
        final int exitTimeoutSeconds = tmStr == null ? DEFAULT_EXIT_TIMEOUT : Integer.valueOf(tmStr);

        log.info("Executing " + cl);
        executor.setStreamHandler(new PumpStreamHandler());
        executor.setProcessDestroyer(
                new ShutdownHookSingleProcessDestroyer("java -jar " + jarToExecute.getName(), exitTimeoutSeconds));
        executor.execute(cl, h);
    }

    /** Stop the process that we started, if any, and wait for it to exit before returning */
    public void stop() {
        if(executor == null) {
            throw new IllegalStateException("Process not started, no Executor set");
        }
        final Object d = executor.getProcessDestroyer();
        if(d instanceof ShutdownHookSingleProcessDestroyer) {
            ((ShutdownHookSingleProcessDestroyer)d).destroyProcess(true);
            log.info("Process destroyed");
        } else {
            throw new IllegalStateException(d + " is not a Runnable, cannot destroy process");
        }
    }
}
