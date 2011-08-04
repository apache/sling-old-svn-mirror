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
package org.apache.sling.launchpad.base.impl.bootstrapcommands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleContext;

public class BootstrapCommandFile {
    /** Name of file used to store our execution timestamp */
    public static final String DATA_FILENAME = BootstrapCommandFile.class.getSimpleName() + "_timestamp.txt";

    /** Prefix for comments in command files */
    public static final String COMMENT_PREFIX = "#";

    private final File commandFile;
    private final Logger logger;

    private static final List<Command> commandPrototypes = new ArrayList<Command>();
    static {
        commandPrototypes.add(new UninstallBundleCommand());
    }

    /** Will load our commands from specified file, if found */
    public BootstrapCommandFile(Logger logger, File cmdFile) {
        this.logger = logger;
        this.commandFile = cmdFile;
    }

    /** True if we have a command file that needs to be executed, based on its
     *  file timestamp and stored timstamp
     */
    boolean anythingToExecute(BundleContext ctx) {
        boolean result = false;
        if(commandFile != null && commandFile.exists() && commandFile.canRead()) {
            final long cmdTs = commandFile.lastModified();
            long lastExecution = 0;
            try {
                lastExecution = loadTimestamp(ctx);
            } catch(IOException ioe) {
                logger.log(Logger.LOG_INFO, "IOException reading timestamp", ioe);
            }
            if(cmdTs > lastExecution) {
                logger.log(Logger.LOG_INFO,
                        "Command file timestamp > stored timestamp, need to execute commands ("
                        + commandFile.getAbsolutePath() + ")");
                result = true;
            }
        }
        if(!result) {
            logger.log(Logger.LOG_INFO,
                    "Command file absent or older than last execution timestamp, nothing to execute ("
                    + commandFile.getAbsolutePath() + ")");
        }
        return result;
    }

    /**
     * Execute commands if needed, and store execution timestamp
     * @return If system bundle needs a restart.
     */
    public boolean execute(BundleContext ctx) throws IOException {
        boolean needsRestart = false;
        if (anythingToExecute(ctx)) {
            InputStream is = null;
            try {
                is = new FileInputStream(commandFile);
                final List<Command> cmds = parse(is);
                for(Command cmd : cmds) {
                    try {
                        logger.log(Logger.LOG_DEBUG, "Executing command: " + cmd);
                        needsRestart |= cmd.execute(logger, ctx);
                    } catch(Exception e) {
                        logger.log(Logger.LOG_WARNING, "Exception in command execution (" + cmd + ") :" + e);
                    }
                }
            } finally {
                if(is != null) {
                    try {
                        is.close();
                    } catch(IOException ignore) {
                        // ignore
                    }
                }
            }

            try {
                storeTimestamp(ctx);
            } catch(IOException ioe) {
                logger.log(Logger.LOG_WARNING, "IOException while storing timestamp", ioe);
            }
        }
        return needsRestart;
    }

    /** Parse commands from supplied input stream.
     *  Does not close the stream */
    List<Command> parse(InputStream is) throws IOException {
        final List<Command> result = new ArrayList<Command>();
        final BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while( (line = r.readLine()) != null) {
            line = line.trim();
            if(line.length() > 0 && !line.startsWith(COMMENT_PREFIX)) {
                Command toAdd = null;
                for(Command proto : commandPrototypes) {
                    toAdd = proto.parse(line);
                    if(toAdd != null) {
                        break;
                    }
                }
                if (toAdd == null) {
                    throw new Command.ParseException("Invalid command '" + line + "'");
                }
                result.add(toAdd);
            }
        }
        return result;
    }

    /** Return the data file to use for our timestamp */
    private File getTimestampFile(BundleContext ctx) {
        return ctx.getDataFile(DATA_FILENAME);
    }

    /** Return our stored timestamp */
    private long loadTimestamp(BundleContext ctx) throws IOException {
        long result = 0;
        final File f = getTimestampFile(ctx);
        if(f.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                byte[] bytes = new byte[20];
                int len = fis.read(bytes);
                if(len > 0) {
                    result = Long.parseLong(new String(bytes, 0, len));
                }
            } finally {
                if(fis != null) {
                    fis.close();
                }
            }
        }
        return result;
    }

    private void storeTimestamp(BundleContext ctx) throws IOException {
        final File f = getTimestampFile(ctx);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            fos.write(String.valueOf(System.currentTimeMillis()).getBytes());
        } finally {
            if(fos != null) {
                fos.close();
            }
        }
    }
}