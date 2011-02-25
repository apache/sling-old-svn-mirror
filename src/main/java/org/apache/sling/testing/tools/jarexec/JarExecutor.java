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
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.exec.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Start a runnable jar by forking a JVM process,
 *  and terminate the process when this VM exits.
 */
public class JarExecutor {
    private static JarExecutor instance;
    private final File jarToExecute;
    private final String javaExecutable;
    private final int serverPort;
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_PORT = 8765;
    public static final String DEFAULT_JAR_FOLDER = "target/dependency";
    public static final String DEFAULT_JAR_NAME_REGEXP = "org.apache.sling.*jar$";
    public static final String PROP_PREFIX = "jar.executor.";
    public static final String PROP_SERVER_PORT = PROP_PREFIX + "server.port";
    public static final String PROP_JAR_FOLDER = PROP_PREFIX + "jar.folder";
    public static final String PROP_JAR_NAME_REGEXP = PROP_PREFIX + "jar.name.regexp";
    
    @SuppressWarnings("serial")
    public static class ExecutorException extends Exception {
        ExecutorException(String reason) {
            super(reason);
        }
        ExecutorException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }
    
    public int getServerPort() {
        return serverPort;
    }

    public static JarExecutor getInstance(Properties config) throws ExecutorException {
        if(instance == null) {
            synchronized (JarExecutor.class) {
                if(instance == null) {
                    instance = new JarExecutor(config);
                }
            }
        }
        return instance;
    }
    
    /** Build a JarExecutor, locate the jar to run, etc */
    private JarExecutor(Properties config) throws ExecutorException {
        final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

        String portStr = config.getProperty(PROP_SERVER_PORT);
        serverPort = portStr == null ? DEFAULT_PORT : Integer.valueOf(portStr);

        javaExecutable = isWindows ? "java.exe" : "java";
        
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
        if(candidates != null) {
            for(String filename : candidates) {
                if(jarPattern.matcher(filename).matches()) {
                    f = new File(jarFolder, filename);
                    break;
                }
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

        final String vmOptions = System.getProperty("jar.executor.vm.options");
        final Executor e = new DefaultExecutor();
        final CommandLine cl = new CommandLine(javaExecutable);
        if (vmOptions != null && vmOptions.length() > 0) {
            // TODO: this will fail if one of the vm options as a quoted value with a space in it, but this is
            // not the case for common usage patterns
            for (String option : StringUtils.split(vmOptions, " ")) {
                cl.addArgument(option);
            }
        }
        cl.addArgument("-jar");
        cl.addArgument(jarToExecute.getAbsolutePath());
        cl.addArgument("-p");
        cl.addArgument(String.valueOf(serverPort));
        log.info("Executing " + cl);
        e.setStreamHandler(new PumpStreamHandler());
        e.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        e.execute(cl, h);
    }
}
