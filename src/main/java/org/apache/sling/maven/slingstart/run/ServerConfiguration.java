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
import java.io.Serializable;

/**
 * A server configuration
 */
public class ServerConfiguration implements Serializable {

    private static final long serialVersionUID = 1922175510880318125L;

    private static final String DEFAULT_VM_OPTS = "-Xmx1024m -XX:MaxPermSize=256m -Djava.awt.headless=true";

    /** The unique id. */
    private String id;

    /** The run mode string. */
    private String runmode;

    /** The port to use. */
    private String port;

    /** The control port to use. */
    private String controlPort;

    /** The context path. */
    private String contextPath;

    /** The vm options. */
    private String vmOpts = DEFAULT_VM_OPTS;

    /** Additional application options. */
    private String opts;

    /** Number of instances. */
    private int instances = 1;

    /** The folder to use. */
    private File folder;

    /**
     * Get the instance id
     * @return The instance id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the instance id
     * @param id New instance id
     */
    public void setId(String id) {
        this.id = id;
    }

    public String getRunmode() {
        return runmode;
    }

    public void setRunmode(final String runmode) {
        this.runmode = runmode;
    }

    public String getPort() {
        return port;
    }

    public void setPort(final String port) {
        this.port = port;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    public String getVmOpts() {
        return vmOpts;
    }

    public void setVmOpts(final String vmOpts) {
        this.vmOpts = vmOpts;
    }

    public String getOpts() {
        return opts;
    }

    public void setOpts(final String opts) {
        this.opts = opts;
    }

    public int getInstances() {
        return this.instances;
    }

    public void setInstances(final int value) {
        this.instances = value;
    }

    public File getFolder() {
        return folder;
    }

    public void setFolder(final File folder) {
        this.folder = folder.getAbsoluteFile();
    }

    public String getControlPort() {
        return controlPort;
    }

    public void setControlPort(String controlPort) {
        this.controlPort = controlPort;
    }

    /**
     * Get the server
     * @return The server
     */
    public String getServer() {
        // hard coded for now
        return "localhost";
    }

    public ServerConfiguration copy() {
        final ServerConfiguration copy = new ServerConfiguration();
        // we do not copy the id
        copy.setRunmode(this.getRunmode());
        copy.setPort(this.getPort());
        copy.setContextPath(this.getContextPath());
        copy.setVmOpts(this.getVmOpts());
        copy.setOpts(this.getOpts());
        copy.setInstances(1);
        copy.setFolder(this.getFolder());
        copy.setControlPort(this.getControlPort());

        return copy;
    }

    @Override
    public String toString() {
        return "LaunchpadConfiguration [id=" + id + ", runmode=" + runmode
                + ", port=" + port + ", controlPort=" + controlPort
                + ", contextPath=" + contextPath
                + ", vmOpts=" + vmOpts + ", opts=" + opts + ", instances="
                + instances + ", folder=" + folder + "]";
    }
}
