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
package org.apache.sling.maven.slingstart.run;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * A running launchpad process.
 */
public class ProcessDescription {

    private final String id;
    private final File directory;
    private final ControlListener listener;
    private volatile Process process;

    public ProcessDescription(final String id, final File directory) throws MojoExecutionException {
        this.id = id;
        this.directory = directory;
        this.listener = new ControlListener(PortHelper.getNextAvailablePort());
    }

    public String getId() {
        return id;
    }

    public File getDirectory() {
        return directory;
    }

    public ControlListener getControlListener() {
        return this.listener;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(final Process process) {
        this.process = process;
    }

    /**
     * Install a shutdown hook
     */
    public void installShutdownHook() {
        final ProcessDescription cfg = this;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if ( cfg.getProcess() != null ) {
                    System.out.println("Terminating launchpad " + cfg.getId());
                    cfg.getProcess().destroy();
                    cfg.setProcess(null);
                }
            }
        });
    }

    @Override
    public String toString() {
        return "RunningProcessDescription [id=" + id + ", directory="
                + directory + ", process=" + process + "]";
    }
}